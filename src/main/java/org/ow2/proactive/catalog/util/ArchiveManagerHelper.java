/*
 * ProActive Parallel Suite(TM):
 * The Open Source library for parallel and distributed
 * Workflows & Scheduling, Orchestration, Cloud Automation
 * and Big Data Analysis on Enterprise Grids & Clouds.
 *
 * Copyright (c) 2007 - 2017 ActiveEon
 * Contact: contact@activeeon.com
 *
 * This library is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation: version 3 of
 * the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 */
package org.ow2.proactive.catalog.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.ow2.proactive.catalog.repository.entity.CatalogObjectRevisionEntity;
import org.springframework.stereotype.Component;
import org.zeroturnaround.zip.ByteSource;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipUtil;


@Component
public class ArchiveManagerHelper {

    /**
     * Compress a list of CatalogObjectRevision files into a ZIP archive
     * @param catalogObjectList the list of catalogObjects to compress
     * @return a byte array corresponding to the archive containing the files
     */
    public byte[] compressZIP(List<CatalogObjectRevisionEntity> catalogObjectList) {

        if (catalogObjectList == null || catalogObjectList.size() == 0) {
            return null;
        }

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

            Stream<ZipEntrySource> streamSources = catalogObjectList.stream()
                                                                    .map(catalogObjectRevision -> new ByteSource(catalogObjectRevision.getCatalogObject()
                                                                                                                                      .getId()
                                                                                                                                      .getName(),
                                                                                                                 catalogObjectRevision.getRawObject()));
            ZipEntrySource[] sources = streamSources.toArray(size -> new ZipEntrySource[size]);
            ZipUtil.pack(sources, byteArrayOutputStream);

            return byteArrayOutputStream.toByteArray();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }

    /**
     * Extract files from an archive
     * @param byteArrayArchive the archive as byte array
     * @return the list of catalogObjects byte arrays
     */
    public List<byte[]> extractZIP(byte[] byteArrayArchive) {

        List<byte[]> filesList = new ArrayList<>();
        if (byteArrayArchive == null) {
            return filesList;
        }

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayArchive)) {
            ZipUtil.iterate(byteArrayInputStream, (in, zipEntry) -> process(in, zipEntry, filesList));
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        return filesList;
    }

    /**
     * Extract ZIP entry into a byte array
     * @param in entry content
     * @param zipEntry entry
     * @param filesList list of files
     */
    private void process(InputStream in, ZipEntry zipEntry, List<byte[]> filesList) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            int data = 0;
            while ((data = in.read()) != -1) {
                outputStream.write(data);
            }
            filesList.add(outputStream.toByteArray());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}
