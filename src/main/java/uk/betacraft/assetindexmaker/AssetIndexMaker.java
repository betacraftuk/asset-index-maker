package uk.betacraft.assetindexmaker;

import org.json.JSONObject;
import uk.betacraft.util.Request;
import uk.betacraft.util.RequestUtil;
import uk.betacraft.util.WebData;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.stream.Stream;

public class AssetIndexMaker {
    public static final String VERSION = "1.0.1";
    static final File SELF; // the running jar file

    static String customUrlBase = null;
    static File exportDestination = new File("assets_missing");
    static File workDirectory = new File(".");
    static File outputTarget = new File("index.json");

    static boolean virtual = false;
    static boolean mapToResources = false;

    static boolean testAvailability = false;
    static boolean exportMissing = false;
    static boolean exportAsObjects = false;

    static boolean debug = false;

    static {
        try {
            SELF = new File(AssetIndexMaker.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        readArguments(args);

        System.out.println("Asset Index Maker v" + VERSION + " loaded");
        System.out.println("--debug " + debug);
        if (debug) {
            System.out.println("--resources " + mapToResources);
            System.out.println("--virtual " + virtual);
            System.out.println("--testAvailability " + testAvailability);
            System.out.println("--customUrl " + customUrlBase);
            System.out.println("--directory " + workDirectory.getPath());
            System.out.println("--output " + outputTarget.getPath());
            System.out.println("--asObjects " + exportAsObjects);
            System.out.println("--exportMissing " + exportMissing);
            System.out.println("--exportMissing " + exportDestination.getPath());
        }

        generateIndex();
    }

    private static void readArguments(String[] args) {
        for (String arg : args) {
            if (arg.equals("--resources")) {
                mapToResources = true;
            } else if (arg.equals("--virtual")) {
                virtual = true;
            } else if (arg.equals("--testAvailability")) {
                testAvailability = true;
            } else if (arg.startsWith("--customUrl=")) {
                customUrlBase = arg.substring("--customUrl=".length());
            } else if (arg.startsWith("--directory=")) {
                workDirectory = new File(arg.substring("--directory=".length()));
            } else if (arg.startsWith("--output=")) {
                outputTarget = new File(arg.substring("--output=".length()));
            } else if (arg.startsWith("--exportMissing")) {
                exportMissing = true;

                if (!arg.contains("="))
                    continue;

                // java is so ugly damn
                String exportPath;
                if (!(exportPath = arg.substring("--exportMissing=".length())).isEmpty()) {
                    exportDestination = new File(exportPath);
                }
            } else if (arg.equals("--asObjects")) {
                exportAsObjects = true;
            } else if (arg.equals("--debug")) {
                debug = true;
            }
        }
    }

    private static void generateIndex() {
        try (Stream<Path> walk = Files.walk(workDirectory.toPath(), 10)) {
            JSONObject objects = new JSONObject();

            walk.forEach(path -> {
                File f = path.toFile();
                String pathString = path.toString();

                if (f.isDirectory() ||
                        // ignore self
                        path.compareTo(SELF.toPath()) == 0 ||
                        path.compareTo(outputTarget.toPath()) == 0 ||
                        // ignore export destination if it's in working directory
                        pathString.startsWith(exportDestination.toPath().toString()) ||
                        // ignore OS-specific junk
                        pathString.contains(".DS_Store") ||
                        pathString.contains("desktop.ini")
                ) {
                    return;
                }

                String sha1 = getSHA1(f);
                String key;

                try {
                    key = URLDecoder.decode(workDirectory.toURI().relativize(path.toUri()).toString(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }

                if (sha1 == null) {
                    System.err.println("Could not read SHA1, skipping: " + key);
                    return;
                }

                JSONObject assetObject = new JSONObject();
                assetObject.put("hash", sha1);
                assetObject.put("size", f.length());

                if (testAvailability)
                    testAvailability(key, sha1, path, assetObject);

                objects.put(key, assetObject);
            });

            JSONObject assetIndex = new JSONObject();
            if (mapToResources)
                assetIndex.put("map_to_resources", true);

            if (virtual)
                assetIndex.put("virtual", true);

            assetIndex.put("objects", objects);

            String json = assetIndex.toString(4);

            Files.write(outputTarget.toPath(), json.getBytes(StandardCharsets.UTF_8));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void testAvailability(String key, String sha1, Path path, JSONObject assetObject) {
        WebData response = RequestUtil.pingGET(
                new Request().setUrl("https://resources.download.minecraft.net/" + sha1.substring(0, 2) + "/" + sha1));

        if (response.successful())
            return;

        System.err.println("Availability test FAILED for: " + key);

        if (customUrlBase != null)
            assetObject.put("custom_url", customUrlBase + "/" + sha1.substring(0, 2) + "/" + sha1);

        if (exportMissing)
            exportMissing(key, sha1, path);
    }

    private static void exportMissing(String key, String sha1, Path path) {
        File destination;
        if (exportAsObjects)
            destination = new File(exportDestination, sha1.substring(0, 2) + "/" + sha1);
        else
            destination = new File(exportDestination, key);

        try {
            destination.getParentFile().mkdirs();

            Files.copy(path, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Throwable t) {
            System.err.println("Asset export FAILED for: " + key);
            t.printStackTrace();
        }
    }

    private static String getSHA1(File file) {
        try {
            InputStream fis = Files.newInputStream(file.toPath());

            byte[] buffer = new byte[1024];
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            int numRead;

            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    messageDigest.update(buffer, 0, numRead);
                }
            } while (numRead != -1);

            fis.close();

            byte[] digest = messageDigest.digest();
            StringBuilder str_result = new StringBuilder();
            for (byte b : digest) {
                str_result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            return str_result.toString();
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }
}