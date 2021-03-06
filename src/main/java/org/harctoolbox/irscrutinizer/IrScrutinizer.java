/*
Copyright (C) 2013, 2014. 2018 Bengt Martensson.

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

package org.harctoolbox.irscrutinizer;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import java.awt.HeadlessException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.harctoolbox.guicomponents.GuiUtils;
import org.harctoolbox.irp.IrpParseException;
import org.harctoolbox.irp.IrpUtils;
import org.xml.sax.SAXException;

/**
 * This class decodes command line parameters and fires up the GUI.
 */
public class IrScrutinizer {

    private final static String backupsuffix = "back";

    private static JCommander argumentParser;
    private static final CommandLineArgs commandLineArgs = new CommandLineArgs();

    private static void usage(int exitcode) {
        StringBuilder str = new StringBuilder(256);
        argumentParser.usage();

        (exitcode == IrpUtils.EXIT_SUCCESS ? System.out : System.err).println(str);
        doExit(exitcode); // placifying FindBugs...
    }

    private static void doExit(int exitcode) {
        System.exit(exitcode);
    }

    /**
     * @param args the command line arguments.
     */
    public static void main(String[] args) {
        argumentParser = new JCommander(commandLineArgs);
        argumentParser.setProgramName(Version.appName);

        try {
            argumentParser.parse(args);
        } catch (ParameterException ex) {
            System.err.println(ex.getMessage());
            usage(IrpUtils.EXIT_USAGE_ERROR);
        }

        if (commandLineArgs.helpRequested)
            usage(IrpUtils.EXIT_SUCCESS);

        if (commandLineArgs.versionRequested) {
            System.out.println(Version.versionString);
            System.out.println("JVM: " + System.getProperty("java.vendor") + " " + System.getProperty("java.version")
                    + " " + System.getProperty("os.name") + "-" + System.getProperty("os.arch"));
            System.out.println();
            System.out.println(Version.licenseString);
            System.exit(IrpUtils.EXIT_SUCCESS);
        }

        if (commandLineArgs.nukeProperties) {
            nukeProperties(true);
            System.exit(IrpUtils.EXIT_SUCCESS);
        }

        try {
            String applicationHome = findApplicationHome(commandLineArgs.applicationHome);
            guiExecute(applicationHome, commandLineArgs.propertiesFilename, commandLineArgs.verbose,
                    commandLineArgs.experimental ? 1 : 0, commandLineArgs.arguments);
        } catch (URISyntaxException ex) {
            System.err.println(ex.getMessage());
            System.exit(IrpUtils.EXIT_FATAL_PROGRAM_FAILURE);
        }
    }

    private static String findApplicationHome(String appHome) throws URISyntaxException {
        String applicationHome = appHome != null ? appHome : System.getenv("IRSCRUTINIZERHOME");
        if (applicationHome == null) {
            URL url = IrScrutinizer.class.getProtectionDomain().getCodeSource().getLocation();
            File dir = new File(url.toURI()).getParentFile();
            applicationHome = (dir.getName().equals("build") || dir.getName().equals("dist"))
                    ? dir.getParent() : dir.getPath();
        }
        if (applicationHome != null && !applicationHome.endsWith(File.separator))
            applicationHome += File.separator;

        return applicationHome;
    }

    private static String nukeProperties(boolean verbose) {
        Props properties = new Props(commandLineArgs.propertiesFilename, commandLineArgs.applicationHome);
        String filename = properties.getFilename();
        String newFilename = filename + "." + backupsuffix;
        if (verbose)
            System.out.println("Renaming the properties file " + filename + " to " + newFilename + ".");
        (new File(filename)).deleteOnExit();
        try {
            return Files.copy((new File(filename)).toPath(), (new File(newFilename)).toPath(), StandardCopyOption.REPLACE_EXISTING).toString();
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
            return null;
        }
    }

    private static void guiExecute(final String applicationHome, final String propsfilename,
            final boolean verbose, final int userlevel, final List<String> arguments) {
        java.awt.EventQueue.invokeLater(() -> {
            try {
                new GuiMain(applicationHome, propsfilename, verbose, userlevel, arguments).setVisible(true);
            } catch (HeadlessException ex) {
                System.err.println("This program does not run in headless mode.");
            } catch (ParseException | IOException /*| URISyntaxException*/ | IrpParseException | RuntimeException ex) {
                GuiUtils.fatal(ex, IrpUtils.EXIT_CONFIG_READ_ERROR, new GuiUtils.EmergencyFixer() {
                    private String backupfile;

                    @Override
                    public void fix() {
                        backupfile = nukeProperties(false);
                    }

                    @Override
                    public String getQuestion() {
                        return "Remove the properites file?";
                    }

                    @Override
                    public String getYesMessage() {
                        return "Renamed the properties file to " + backupfile + ".";
                    }

                    @Override
                    public String getNoMessage() {
                        return null;
                    }
                });
            } catch (ParserConfigurationException ex) {
                GuiUtils.fatal(ex, IrpUtils.EXIT_INTERNAL_FAILURE);
            } catch (SAXException ex) {
                GuiUtils.fatal(ex, IrpUtils.EXIT_XML_ERROR);
            }
        });
    }

    private IrScrutinizer() {
    }

    private final static class CommandLineArgs {

        @Parameter(names = {"-h", "--help", "-?"}, description = "Display help message")
        private boolean helpRequested = false;

        @Parameter(names = {"-H", "--home", "--applicationhome", "--apphome"}, description = "Set application home (where files are located)")
        private String applicationHome = null;

        @Parameter(names = {"--nuke-properties"}, description = "Get rid of present properties file")
        private boolean nukeProperties = false;

        @Parameter(names = {"-p", "--properties"}, description = "Pathname of properties file")
        private String propertiesFilename = null;

        @Parameter(names = {"-V", "--version"}, description = "Display version information")
        private boolean versionRequested;

        @Parameter(names = {"-v", "--verbose"}, description = "Have some commands executed verbosely")
        private boolean verbose;

        @Parameter(names = {"-x", "--experimental"}, description = "Enable experimental features", hidden = true)
        private boolean experimental;

        @Parameter(description = "Arguments to the program")
        private List<String> arguments = new ArrayList<>(4);
    }
}
