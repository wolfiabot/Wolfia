/*
 * Copyright (C) 2017 Dennis Neufeld
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package space.npstr.wolfia.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by napster on 06.07.17.
 * <p>
 * Thanks Fred
 */
public class SimpleCache {

    private static final Map<String, File> cachedURLFiles = new HashMap<>();

    private SimpleCache() {
    }

    public static File getImageFromURL(final String url) {
        if (cachedURLFiles.containsKey(url) && cachedURLFiles.get(url).exists()) {
            //Already cached
            return cachedURLFiles.get(url);
        } else {
            final InputStream is;
            final FileOutputStream fos;
            File tmpFile = null;
            try {
                final Matcher matcher = Pattern.compile("(\\.\\w+$)").matcher(url);
                final String type = matcher.find() ? matcher.group(1) : "";
                tmpFile = File.createTempFile(UUID.randomUUID().toString(), type);
                //noinspection ConstantConditions
                is = new URL(url).openStream();
                final FileWriter writer = new FileWriter(tmpFile);
                fos = new FileOutputStream(tmpFile);

                final byte[] buffer = new byte[1024 * 10];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                is.close();
                fos.close();

                cachedURLFiles.put(url, tmpFile);
                return tmpFile;
            } catch (final IOException ex) {
                if (tmpFile != null) tmpFile.delete();
                throw new RuntimeException(ex);
            }
        }
    }
}
