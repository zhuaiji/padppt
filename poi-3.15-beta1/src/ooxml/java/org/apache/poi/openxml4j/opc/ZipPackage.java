/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.openxml4j.opc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.InvalidOperationException;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.openxml4j.exceptions.ODFNotOfficeXmlFileException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JRuntimeException;
import org.apache.poi.openxml4j.opc.internal.ContentTypeManager;
import org.apache.poi.openxml4j.opc.internal.FileHelper;
import org.apache.poi.openxml4j.opc.internal.MemoryPackagePart;
import org.apache.poi.openxml4j.opc.internal.PartMarshaller;
import org.apache.poi.openxml4j.opc.internal.ZipContentTypeManager;
import org.apache.poi.openxml4j.opc.internal.ZipHelper;
import org.apache.poi.openxml4j.opc.internal.marshallers.ZipPartMarshaller;
import org.apache.poi.openxml4j.util.ZipEntrySource;
import org.apache.poi.openxml4j.util.ZipFileZipEntrySource;
import org.apache.poi.openxml4j.util.ZipInputStreamZipEntrySource;
import org.apache.poi.openxml4j.util.ZipSecureFile.ThresholdInputStream;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.TempFile;

/**
 * Physical zip package.
 */
@SuppressWarnings("deprecation")
public final class ZipPackage extends Package {
    private static POILogger logger = POILogFactory.getLogger(ZipPackage.class);

    /**
     * Zip archive, as either a file on disk,
     *  or a stream
     */
    private final ZipEntrySource zipArchive;

    /**
     * Constructor. Creates a new, empty ZipPackage.
     */
    public ZipPackage() {
        super(defaultPackageAccess);
        this.zipArchive = null;

        try {
            this.contentTypeManager = new ZipContentTypeManager(null, this);
        } catch (InvalidFormatException e) {
            logger.log(POILogger.WARN,"Could not parse ZipPackage", e);
        }
    }

    /**
     * Constructor. Opens a Zip based Open XML document from
     *  an InputStream.
     *
     * @param in
     *            Zip input stream to load.
     * @param access
     *            The package access mode.
     * @throws IllegalArgumentException
     *             If the specified input stream not an instance of
     *             ZipInputStream.
     */
    ZipPackage(InputStream in, PackageAccess access) throws IOException {
        super(access);
        @SuppressWarnings("resource")
        ThresholdInputStream zis = ZipHelper.openZipStream(in);
        this.zipArchive = new ZipInputStreamZipEntrySource(zis);
    }

    /**
     * Constructor. Opens a Zip based Open XML document from a file.
     *
     * @param path
     *            The path of the file to open or create.
     * @param access
     *            The package access mode.
     */
    ZipPackage(String path, PackageAccess access) {
        this(new File(path), access);
    }

    /**
     * Constructor. Opens a Zip based Open XML document from a File.
     *
     * @param file
     *            The file to open or create.
     * @param access
     *            The package access mode.
     */
    @SuppressWarnings("resource")
    ZipPackage(File file, PackageAccess access) {
        super(access);

        ZipEntrySource ze;
        try {
            final ZipFile zipFile = ZipHelper.openZipFile(file);
            ze = new ZipFileZipEntrySource(zipFile);
        } catch (IOException e) {
            // probably not happening with write access - not sure how to handle the default read-write access ...
            if (access == PackageAccess.WRITE) {
                throw new InvalidOperationException("Can't open the specified file: '" + file + "'", e);
            }
            logger.log(POILogger.ERROR, "Error in zip file "+file+" - falling back to stream processing (i.e. ignoring zip central directory)");
            // some zips can't be opened via ZipFile in JDK6, as the central directory
            // contains either non-latin entries or the compression type can't be handled
            // the workaround is to iterate over the stream and not the directory
            FileInputStream fis = null;
            ThresholdInputStream zis = null;
            try {
                fis = new FileInputStream(file);
                zis = ZipHelper.openZipStream(fis);
                ze = new ZipInputStreamZipEntrySource(zis);
            } catch (IOException e2) {
                if (zis != null) {
                    try {
                        zis.close();
                    } catch (IOException e3) {
                        throw new InvalidOperationException("Can't open the specified file: '" + file + "'"+
                                " and couldn't close the file input stream", e);
                    }
                } else if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e3) {
                        throw new InvalidOperationException("Can't open the specified file: '" + file + "'"+
                                " and couldn't close the file input stream", e);
                    }
                }
                throw new InvalidOperationException("Can't open the specified file: '" + file + "'", e);
            }
        }
        this.zipArchive = ze;
    }

    /**
     * Constructor. Opens a Zip based Open XML document from
     *  a custom ZipEntrySource, typically an open archive
     *  from another system
     *
     * @param zipEntry
     *            Zip data to load.
     * @param access
     *            The package access mode.
     */
    ZipPackage(ZipEntrySource zipEntry, PackageAccess access) {
        super(access);
        this.zipArchive = zipEntry;
    }

    /**
     * Retrieves the parts from this package. We assume that the package has not
     * been yet inspect to retrieve all the parts, this method will open the
     * archive and look for all parts contain inside it. If the package part
     * list is not empty, it will be emptied.
     *
     * @return All parts contain in this package.
     * @throws InvalidFormatException
     *             Throws if the package is not valid.
     */
    @Override
    protected PackagePart[] getPartsImpl() throws InvalidFormatException {
        if (this.partList == null) {
            // The package has just been created, we create an empty part
            // list.
            this.partList = new PackagePartCollection();
        }

        if (this.zipArchive == null) {
            return this.partList.values().toArray(
                    new PackagePart[this.partList.values().size()]);
        }

        // First we need to parse the content type part
        Enumeration<? extends ZipEntry> entries = this.zipArchive.getEntries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.getName().equalsIgnoreCase(
                    ContentTypeManager.CONTENT_TYPES_PART_NAME)) {
                try {
                    this.contentTypeManager = new ZipContentTypeManager(
                            getZipArchive().getInputStream(entry), this);
                } catch (IOException e) {
                    throw new InvalidFormatException(e.getMessage());
                }
                break;
            }
        }

        // At this point, we should have loaded the content type part
        if (this.contentTypeManager == null) {
            // Is it a different Zip-based format?
            int numEntries = 0;
            boolean hasMimetype = false;
            boolean hasSettingsXML = false;
            entries = this.zipArchive.getEntries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().equals("mimetype")) {
                    hasMimetype = true;
                }
                if (entry.getName().equals("settings.xml")) {
                    hasSettingsXML = true;
                }
                numEntries++;
            }
            if (hasMimetype && hasSettingsXML) {
                throw new ODFNotOfficeXmlFileException(
                   "The supplied data appears to be in ODF (Open Document) Format. " +
                   "Formats like these (eg ODS, ODP) are not supported, try Apache ODFToolkit");
            }
            if (numEntries == 0) {
                throw new NotOfficeXmlFileException(
                   "No valid entries or contents found, this is not a valid OOXML " +
                   "(Office Open XML) file");
            }

            // Fallback exception
            throw new InvalidFormatException(
                    "Package should contain a content type part [M1.13]");
        }

        // Now create all the relationships
        // (Need to create relationships before other
        //  parts, otherwise we might create a part before
        //  its relationship exists, and then it won't tie up)
        entries = this.zipArchive.getEntries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            PackagePartName partName = buildPartName(entry);
            if(partName == null) continue;

            // Only proceed for Relationships at this stage
            String contentType = contentTypeManager.getContentType(partName);
            if (contentType != null && contentType.equals(ContentTypes.RELATIONSHIPS_PART)) {
                try {
                    partList.put(partName, new ZipPackagePart(this, entry,
                                                              partName, contentType));
                } catch (InvalidOperationException e) {
                    throw new InvalidFormatException(e.getMessage());
                }
            }
        }

        // Then we can go through all the other parts
        entries = this.zipArchive.getEntries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            PackagePartName partName = buildPartName(entry);
            if(partName == null) continue;

            String contentType = contentTypeManager
                    .getContentType(partName);
            if (contentType != null && contentType.equals(ContentTypes.RELATIONSHIPS_PART)) {
                // Already handled
            }
            else if (contentType != null) {
                try {
                    partList.put(partName, new ZipPackagePart(this, entry,
                            partName, contentType));
                } catch (InvalidOperationException e) {
                    throw new InvalidFormatException(e.getMessage());
                }
            } else {
                throw new InvalidFormatException(
                        "The part "
                                + partName.getURI().getPath()
                                + " does not have any content type ! Rule: Package require content types when retrieving a part from a package. [M.1.14]");
            }
        }

        return partList.values().toArray(new ZipPackagePart[partList.size()]);
    }

    /**
     * Builds a PackagePartName for the given ZipEntry,
     *  or null if it's the content types / invalid part
     */
    private PackagePartName buildPartName(ZipEntry entry) {
        try {
            // We get an error when we parse [Content_Types].xml
            // because it's not a valid URI.
            if (entry.getName().equalsIgnoreCase(
                    ContentTypeManager.CONTENT_TYPES_PART_NAME)) {
                return null;
            }
            return PackagingURIHelper.createPartName(ZipHelper
                    .getOPCNameFromZipItemName(entry.getName()));
        } catch (Exception e) {
            // We assume we can continue, even in degraded mode ...
            logger.log(POILogger.WARN,"Entry "
                                      + entry.getName()
                                      + " is not valid, so this part won't be add to the package.", e);
            return null;
        }
    }

    /**
     * Create a new MemoryPackagePart from the specified URI and content type
     *
     *
     * aram partName The part URI.
     *
     * @param contentType
     *            The part content type.
     * @return The newly created zip package part, else <b>null</b>.
     */
    @Override
    protected PackagePart createPartImpl(PackagePartName partName,
            String contentType, boolean loadRelationships) {
        if (contentType == null)
            throw new IllegalArgumentException("contentType");

        if (partName == null)
            throw new IllegalArgumentException("partName");

        try {
            return new MemoryPackagePart(this, partName, contentType,
                    loadRelationships);
        } catch (InvalidFormatException e) {
            logger.log(POILogger.WARN, e);
            return null;
        }
    }

    /**
     * Delete a part from the package
     *
     * @throws IllegalArgumentException
     *             Throws if the part URI is nulll or invalid.
     */
    @Override
    protected void removePartImpl(PackagePartName partName) {
        if (partName == null)
            throw new IllegalArgumentException("partUri");
    }

    /**
     * Flush the package. Do nothing.
     */
    @Override
    protected void flushImpl() {
        // Do nothing
    }

    /**
     * Close and save the package.
     *
     * @see #close()
     */
    @Override
    protected void closeImpl() throws IOException {
        // Flush the package
        flush();

		// Save the content
		if (this.originalPackagePath != null
				&& !"".equals(this.originalPackagePath)) {
			File targetFile = new File(this.originalPackagePath);
			if (targetFile.exists()) {
				// Case of a package previously open

				File tempFile = TempFile.createTempFile(
						generateTempFileName(FileHelper
								.getDirectory(targetFile)), ".tmp");

				// Save the final package to a temporary file
				try {
					save(tempFile);
					
					// Close the current zip file, so we can
					//  overwrite it on all platforms
					this.zipArchive.close();
					// Copy the new file over the old one
					FileHelper.copyFile(tempFile, targetFile);
				} finally {
					// Either the save operation succeed or not, we delete the
					// temporary file
					if (!tempFile.delete()) {
						logger
								.log(POILogger.WARN,"The temporary file: '"
										+ targetFile.getAbsolutePath()
										+ "' cannot be deleted ! Make sure that no other application use it.");
					}
				}
			} else {
				throw new InvalidOperationException(
						"Can't close a package not previously open with the open() method !");
			}
		} 
	}

	/**
	 * Create a unique identifier to be use as a temp file name.
	 *
	 * @return A unique identifier use to be use as a temp file name.
	 */
	private synchronized String generateTempFileName(File directory) {
		File tmpFilename;
		do {
			tmpFilename = new File(directory.getAbsoluteFile() + File.separator
					+ "OpenXML4J" + System.nanoTime());
		} while (tmpFilename.exists());
		return FileHelper.getFilename(tmpFilename.getAbsoluteFile());
	}

	/**
	 * Close the package without saving the document. Discard all the changes
	 * made to this package.
	 */
	@Override
	protected void revertImpl() {
		try {
			if (this.zipArchive != null)
				this.zipArchive.close();
		} catch (IOException e) {
			// Do nothing, user dont have to know
		}
	}

    /**
     * Implement the getPart() method to retrieve a part from its URI in the
     * current package
     *
     *
     * @see #getPart(PackageRelationship)
     */
    @Override
    protected PackagePart getPartImpl(PackagePartName partName) {
        if (partList.containsKey(partName)) {
            return partList.get(partName);
        }
        return null;
    }

	/**
	 * Save this package into the specified stream
	 *
	 *
	 * @param outputStream
	 *            The stream use to save this package.
	 *
	 * @see #save(OutputStream)
	 */
	@Override
	public void saveImpl(OutputStream outputStream) {
		// Check that the document was open in write mode
		throwExceptionIfReadOnly();

		final ZipOutputStream zos;
		try {
			if (!(outputStream instanceof ZipOutputStream))
				zos = new ZipOutputStream(outputStream);
			else
				zos = (ZipOutputStream) outputStream;

			// If the core properties part does not exist in the part list,
			// we save it as well
			if (this.getPartsByRelationshipType(PackageRelationshipTypes.CORE_PROPERTIES).size() == 0 &&
                this.getPartsByRelationshipType(PackageRelationshipTypes.CORE_PROPERTIES_ECMA376).size() == 0    ) {
				logger.log(POILogger.DEBUG,"Save core properties part");
				
				// Ensure that core properties are added if missing
				getPackageProperties();
				// Add core properties to part list ...
				addPackagePart(this.packageProperties);
				// ... and to add its relationship ...
				this.relationships.addRelationship(this.packageProperties
						.getPartName().getURI(), TargetMode.INTERNAL,
						PackageRelationshipTypes.CORE_PROPERTIES, null);
				// ... and the content if it has not been added yet.
				if (!this.contentTypeManager
						.isContentTypeRegister(ContentTypes.CORE_PROPERTIES_PART)) {
					this.contentTypeManager.addContentType(
							this.packageProperties.getPartName(),
							ContentTypes.CORE_PROPERTIES_PART);
				}
			}

			// Save package relationships part.
			logger.log(POILogger.DEBUG,"Save package relationships");
			ZipPartMarshaller.marshallRelationshipPart(this.getRelationships(),
					PackagingURIHelper.PACKAGE_RELATIONSHIPS_ROOT_PART_NAME,
					zos);

			// Save content type part.
			logger.log(POILogger.DEBUG,"Save content types part");
			this.contentTypeManager.save(zos);

			// Save parts.
			for (PackagePart part : getParts()) {
				// If the part is a relationship part, we don't save it, it's
				// the source part that will do the job.
				if (part.isRelationshipPart())
					continue;

				logger.log(POILogger.DEBUG,"Save part '"
						+ ZipHelper.getZipItemNameFromOPCName(part
								.getPartName().getName()) + "'");
				PartMarshaller marshaller = partMarshallers
						.get(part._contentType);
				if (marshaller != null) {
					if (!marshaller.marshall(part, zos)) {
						throw new OpenXML4JException(
								"The part "
										+ part.getPartName().getURI()
										+ " fail to be saved in the stream with marshaller "
										+ marshaller);
					}
				} else {
					if (!defaultPartMarshaller.marshall(part, zos))
						throw new OpenXML4JException(
								"The part "
										+ part.getPartName().getURI()
										+ " fail to be saved in the stream with marshaller "
										+ defaultPartMarshaller);
				}
			}
			zos.close();
		} catch (OpenXML4JRuntimeException e) {
			// no need to wrap this type of Exception
			throw e;
		} catch (Exception e) {
            throw new OpenXML4JRuntimeException(
                    "Fail to save: an error occurs while saving the package : "
							+ e.getMessage(), e);
		}
    }

    /**
     * Get the zip archive
     *
     * @return The zip archive.
     */
    public ZipEntrySource getZipArchive() {
        return zipArchive;
    }
}
