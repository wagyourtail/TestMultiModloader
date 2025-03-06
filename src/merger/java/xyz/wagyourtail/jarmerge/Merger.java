package xyz.wagyourtail.jarmerge;

import io.github.prcraftmc.classdiff.ClassDiffer;
import io.github.prcraftmc.classdiff.format.DiffWriter;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.objectweb.asm.ClassReader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Merger {

    public static void main(String[] args) throws IOException {
        Path common = Path.of(args[0]);
        Path output = Path.of(args[1]);

        Set<String> files = new HashSet<>();
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(output, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {

            try (ZipFile commonJar = new ZipFile(common)) {
                commonJar.getEntries().asIterator().forEachRemaining(entry -> {
                    try {
                        if (entry.isDirectory()) return;
                        System.out.println("Copying " + entry.getName());
                        files.add(entry.getName());
                        zos.putNextEntry(new ZipEntry(entry.getName()));
                        commonJar.getInputStream(entry).transferTo(zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        System.out.println("Failed to copy " + entry.getName());
                        throw new UncheckedIOException(e);
                    }
                });

                for (int i = 2; i < args.length; i++) {
                    String platform = args[i++];
                    Path input = Path.of(args[i]);

                    try (ByteArrayOutputStream platformDiff = new ByteArrayOutputStream(); ZipOutputStream platformZos = new ZipOutputStream(platformDiff)) {

                        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(Files.newInputStream(input))) {
                            for (ZipArchiveEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
                                if (!entry.getName().endsWith(".class")) {
                                    if (!files.contains(entry.getName())) {
                                        if (entry.isDirectory()) continue;
                                        zos.putNextEntry(new ZipEntry(entry.getName()));
                                        zis.transferTo(zos);
                                        zos.closeEntry();
                                    }
                                } else {
                                    String className = entry.getName().substring(0, entry.getName().length() - 6);
                                    if (commonJar.getEntry(entry.getName()) != null) {
                                        byte[] commonBytes = commonJar.getInputStream(commonJar.getEntry(entry.getName())).readAllBytes();
                                        byte[] platformBytes = zis.readAllBytes();
                                        if (Arrays.equals(commonBytes, platformBytes)) continue;
                                        ClassReader commonReader = new ClassReader(commonBytes);
                                        ClassReader platformReader = new ClassReader(platformBytes);
                                        DiffWriter diffWriter = new DiffWriter();
                                        ClassDiffer.diff(commonReader, platformReader, diffWriter);
                                        platformZos.putNextEntry(new ZipArchiveEntry(className + ".class.cdiff"));
                                        platformZos.write(diffWriter.toByteArray());
                                        platformZos.closeEntry();
                                    } else if (!files.contains(entry.getName())) {
                                        try {
                                            System.out.println("Copying " + entry.getName());
                                            files.add(entry.getName());
                                            zos.putNextEntry(new ZipEntry(entry.getName()));
                                            zis.transferTo(zos);
                                            zos.closeEntry();
                                        } catch (IOException e) {
                                            System.out.println("Failed to copy " + entry.getName());
                                            throw new UncheckedIOException(e);
                                        }
                                    }
                                }
                            }
                        }

                        platformZos.close();
                        zos.putNextEntry(new ZipArchiveEntry(platform + ".patches.zip"));
                        platformDiff.writeTo(zos);
                        zos.closeEntry();
                    }
                }
            }
        }
    }

}
