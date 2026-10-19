import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** Runs Bazaar's official bundle-signer with the already configured Vista release key. */
class GenerateBazaarBin {
    private static final String EXPECTED_CERT_SHA256 =
        "45ed8d0152484eba6805a575a1e67778f0ad8e5b5f970dfe8bd07edc141684ae";

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("Expected: signer.jar production.aab empty-output-directory");
        }
        Path root = Path.of("").toAbsolutePath().normalize();
        Path signer = Path.of(args[0]).toAbsolutePath().normalize();
        Path bundle = Path.of(args[1]).toAbsolutePath().normalize();
        Path output = Path.of(args[2]).toAbsolutePath().normalize();
        if (!Files.isRegularFile(signer) || !Files.isRegularFile(bundle)) {
            throw new IllegalArgumentException("Signer JAR or production AAB is missing");
        }
        if (Files.exists(output)) {
            try (var entries = Files.list(output)) {
                if (entries.findAny().isPresent()) {
                    throw new IllegalArgumentException("Output directory must be empty");
                }
            }
        } else {
            Files.createDirectories(output);
        }

        Properties properties = new Properties();
        for (Path candidate : List.of(
                root.resolve("key.properties"),
                root.resolve("app/key.properties"),
                Path.of("E:/vista/android/key.properties"))) {
            if (Files.isRegularFile(candidate)) {
                try (InputStream input = Files.newInputStream(candidate)) {
                    properties.load(input);
                }
                break;
            }
        }
        String rawStore = required(value("VISTA_SIGNING_STORE_FILE", "storeFile", properties));
        String storePassword = required(value("VISTA_SIGNING_STORE_PASSWORD", "storePassword", properties));
        String alias = required(value("VISTA_SIGNING_KEY_ALIAS", "keyAlias", properties));
        String keyPassword = required(value("VISTA_SIGNING_KEY_PASSWORD", "keyPassword", properties));
        Path store = resolveStore(root, rawStore);
        if (!Files.isRegularFile(store)) {
            throw new IllegalStateException("Configured production keystore does not exist");
        }

        // Refuse to sign with any key other than the verified Flutter production identity.
        KeyStore keyStore = KeyStore.getInstance(store.toFile(), storePassword.toCharArray());
        var certificate = keyStore.getCertificate(alias);
        if (certificate == null) {
            throw new IllegalStateException("Configured release alias was not found");
        }
        String actualFingerprint = HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded()));
        if (!EXPECTED_CERT_SHA256.equals(actualFingerprint)) {
            throw new IllegalStateException("Release certificate does not match Flutter production");
        }

        List<String> command = new ArrayList<>(List.of(
            Path.of(System.getProperty("java.home"), "bin", "java.exe").toString(),
            "-jar", signer.toString(), "genbin",
            "--bundle", bundle.toString(), "--bin", output.toString(),
            "--v2-signing-enabled", "true", "--v3-signing-enabled", "false",
            "--ks", store.toString(), "--ks-key-alias", alias,
            "--ks-pass", "env:VISTA_BAZAAR_STORE_PASSWORD",
            "--key-pass", "env:VISTA_BAZAAR_KEY_PASSWORD"
        ));
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        Map<String, String> environment = builder.environment();
        environment.put("VISTA_BAZAAR_STORE_PASSWORD", storePassword);
        environment.put("VISTA_BAZAAR_KEY_PASSWORD", keyPassword);
        properties.clear();
        Process process = builder.start();
        // Do not relay tool output: keystore details and signing material stay out of logs.
        process.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IllegalStateException("Official bundle-signer failed (exit " + exit + ")");
        }
        try (var entries = Files.list(output)) {
            List<Path> bins = entries.filter(path -> path.getFileName().toString().endsWith(".bin"))
                .toList();
            if (bins.isEmpty()) {
                throw new IllegalStateException("Official bundle-signer produced no .bin artifact");
            }
            for (Path bin : bins) {
                System.out.println(bin + " (" + Files.size(bin) + " bytes)");
            }
        }
    }

    private static String value(String envName, String propertyName, Properties properties) {
        String environment = System.getenv(envName);
        return environment != null && !environment.isBlank()
            ? environment : properties.getProperty(propertyName);
    }

    private static String required(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Production signing configuration is incomplete");
        }
        return value;
    }

    private static Path resolveStore(Path root, String raw) {
        Path supplied = Path.of(raw);
        if (supplied.isAbsolute()) return supplied;
        return List.of(
            root.resolve("app").resolve(raw),
            root.resolve(raw),
            root.resolve("app/src/main").resolve(raw),
            Path.of("E:/vista/android/app").resolve(raw)
        ).stream().filter(Files::exists).findFirst().orElse(root.resolve("app").resolve(raw));
    }
}
