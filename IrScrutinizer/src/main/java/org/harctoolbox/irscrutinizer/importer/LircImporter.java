/*
Copyright (C) 2013 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
*/

package org.harctoolbox.irscrutinizer.importer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.util.HashMap;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.girr.Command;
import org.harctoolbox.jirc.ConfigFile;
import org.harctoolbox.jirc.IrNCode;
import org.harctoolbox.jirc.IrRemote;

/**
 * This class is basically a wrapper around Jirc.
 */
public class LircImporter extends RemoteSetImporter implements IReaderImporter, Serializable {
    public static final String remotesUrl = "http://lirc.sourceforge.net/remotes/";
    public static final String homeUrl = "http://www.lirc.org/";

    private static final int debug = 0;

    public LircImporter() {
        super();
    }

    private void load(HashMap<String,IrRemote> lircRemotes, String origin) {
        //this.lircRemotes = lircRemotes;
        prepareLoad(origin);
        remoteSet = IrRemote.newRemoteSet(lircRemotes, origin, isInvokeDecodeIr(), getCreatingUser(), true, 0 /* debug */);
        for (IrRemote remote : lircRemotes.values()) {
            for (IrNCode code : remote.getCodes()) {
                IrSignal irSignal = remote.toIrSignal(code, true, debug);
                if (irSignal != null) {
                    Command command = new Command(code.getName(), origin /* comment */, irSignal, isGenerateCcf(), isInvokeDecodeIr());
                    addCommand(command);
                }
            }
        }
    }

    @Override
    public void load(InputStream inputStream, String origin, String charsetName) throws IOException {
        load(ConfigFile.readConfig(inputStream, origin, charsetName, false), origin);
    }

    @Override
    public void load(String input, String origin) throws IOException {
        load(ConfigFile.readConfig(input, origin, false), origin);
    }

    @Override
    public void load(File file, String origin, String charsetName) throws IOException {
        load(ConfigFile.readConfig(file, charsetName, false), origin);
    }

    @Override
    public void load(Reader reader, String origin) throws IOException {
        load(ConfigFile.readConfig(reader, origin, false), origin);
    }

    public void load(File file) throws IOException {
        load(ConfigFile.readConfig(file), file.getCanonicalPath());
    }

    @Override
    public boolean canImportDirectories() {
        return true;
    }

    @Override
    public String[][] getFileExtensions() {
        return new String[][]{
            // Lirc uses lirc.conf as "file extension". However,
            // javax.swing.filechooser.FileNameExtensionFilter does not allow
            // file extensions with a dot. Not important enough to fix.
            //new String[]{"Lircd conf files (*.lircd.conf)", "lircd.conf" },
            new String[]{"Conf files (*.conf)", "conf" },
            new String[]{"Lirc files (*.lirc)", "lirc" }
        };
    }

    @Override
    public String getFormatName() {
        return "LIRC";
    }
}
