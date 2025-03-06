package org.example.agent;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.spongepowered.asm.mixin.connect.IMixinConnector;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PlatformAgentHook implements IMixinConnector {

    public static final String platformName = getPlatformName();

    private static String getPlatformName() {
        try {
            Class.forName("net.fabricmc.loader.FabricLoader");
            return "fabric";
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class.forName("net.minecraftforge.fml.loading.FMLLoader");
            return "forge";
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class.forName("net.neoforged.fml.loading.FMLLoader");
            return "neoforge";
        } catch (ClassNotFoundException ignored) {
        }
        throw new IllegalStateException("Unsupported platform");
    }

    private static final String manifest = """
            Manifest-Version: 1.0
            Premain-Class: org.example.agent.PlatformAgent
            Can-Redefine-Classes: true
            Can-Retransform-Classes: true
            
            """.stripIndent();

    private static byte[] getResourceBytes(String path) throws IOException {
        try (InputStream is = PlatformAgentHook.class.getResourceAsStream(path)) {
            return is.readAllBytes();
        }
    }


    @Override
    public void connect() {
        System.out.println("Connecting transform agent");
        System.setProperty("platform.agent", platformName);

        try {

            Path output = Path.of("config/platform-agent/agent.jar");
            Files.createDirectories(output.getParent());

            try (ZipOutputStream jos = new JarOutputStream(Files.newOutputStream(output, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                jos.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
                jos.write(manifest.getBytes());
                jos.closeEntry();

                jos.putNextEntry(new ZipEntry("org/example/agent/PlatformAgent.class"));
                jos.write(getResourceBytes("/org/example/agent/PlatformAgent.class"));
                jos.closeEntry();

                jos.putNextEntry(new ZipEntry("org/example/agent/PlatformTransformer.class"));
                jos.write(getResourceBytes("/org/example/agent/PlatformTransformer.class"));
                jos.closeEntry();

                jos.putNextEntry(new ZipEntry(platformName + ".patches.zip"));
                jos.write(getResourceBytes("/" + platformName + ".patches.zip"));
                jos.closeEntry();
            }

            List<Path> deps = new ArrayList<>();

            try (InputStream is = PlatformAgentHook.class.getResourceAsStream("/META-INF/jarjar/metadata.json")) {
                JsonObject metadata = JsonParser.parseString(new String(is.readAllBytes())).getAsJsonObject();
                JsonArray jars = metadata.getAsJsonArray("jars");

                for (int i = 0; i < jars.size(); i++) {
                    JsonObject jar = jars.get(i).getAsJsonObject();
                    String path = jar.get("path").getAsString();
                    Path dep = output.getParent().resolve(path.substring(path.lastIndexOf('/') + 1));

                    try (InputStream is2 = PlatformAgentHook.class.getResourceAsStream("/" + path)) {
                        assert is2 != null;
                        Files.copy(is2, dep, StandardCopyOption.REPLACE_EXISTING);
                    }

                    deps.add(dep);
                }
            }

            if (!platformName.equals("fabric")) {
                ForgeUnsafeHelper.addToFallbackClassloader(output);
                for (Path dep : deps) {
                    ForgeUnsafeHelper.addToFallbackClassloader(dep);
                }

                ForgeUnsafeHelper.addReadsAllUnnamed(IMixinInfo.class.getModule());

//                ByteBuddyAgent.attach(output.toAbsolutePath().toFile(), String.valueOf(ProcessHandle.current().pid()), platformName);
            } else {
                UnsafeHelper.addToSystemClassLoader(output.toUri().toURL());
                for (Path dep : deps) {
                    UnsafeHelper.addToSystemClassLoader(dep.toUri().toURL());
                }
            }

            ByteBuddyAgent.install();
            Instrumentation instrumentation = ByteBuddyAgent.getInstrumentation();
            PlatformAgent.agentmain(platformName, instrumentation);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

}
