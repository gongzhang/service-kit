package co.gongzh.servicekit;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author Gong Zhang
 */
public class AtomicFileHelper {

    @NotNull
    public static Consumer<String> logger = System.err::println;

    public interface Writer {
        void write(OutputStream os) throws IOException;
    }

    public interface Reader<D, T> {
        T read(D data) throws Exception;
    }

    private static final String OLD_FILE_SUFFIX = ".old";
    private static final String CORRUPTED_FILE_SUFFIX = ".corrupted";

    @NotNull
    private static File getOldFile(@NotNull File file) {
        return new File(file.getParent(), file.getName() + OLD_FILE_SUFFIX);
    }

    @NotNull
    private static File getCorruptedFile(@NotNull File file) {
        return new File(file.getParent(), file.getName() + CORRUPTED_FILE_SUFFIX);
    }

    public static void write(@NotNull final File file, @NotNull final Writer writer) throws IOException {
        // 1. replace old file
        final File oldFile = getOldFile(file);
        if (file.exists()) {
            try {
                Files.move(file.toPath(), oldFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception ex) {
                logger.accept("Failed to rename " + file.getName() + " to " + oldFile.getName() + ": " + ex.toString());
            }
        }

        // 3. write new file
        try (FileOutputStream fos = new FileOutputStream(file)) {
            writer.write(fos);
        }
    }

    public static void write(@NotNull final File file, @NotNull byte[] bytes) throws IOException {
        write(file, w -> {
            w.write(bytes);
        });
    }

    public static void writeUTF8(@NotNull final File file, @NotNull String text) throws IOException {
        write(file, text.getBytes(StandardCharsets.UTF_8));
    }

    public static void writeJSON(@NotNull final File file, @NotNull JSONObject object) throws IOException {
        writeUTF8(file, object.toString());
    }

    public static void writeJSON(@NotNull final File file, @NotNull JSONObject object, boolean pretty) throws IOException {
        writeUTF8(file, object.toString(pretty ? 2 : 0));
    }

    public static <T> Optional<T> read(@NotNull final File file, @NotNull final Reader<byte[], T> reader) {
        // 1. read from designate file
        if (file.exists()) {
            try {
                byte[] bytes = Files.readAllBytes(file.toPath());
                return Optional.of(reader.read(bytes));
            } catch (Exception ex) {
                logger.accept("Failed to read " + file.getName() + ": " + ex.toString());

                // delete corrupted file
                File corrupted = getCorruptedFile(file);
                try {
                    Files.move(file.toPath(), corrupted.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    logger.accept("Corrupted file deleted: " + file.getName());
                } catch (IOException ex2) {
                    logger.accept("Failed to deleted corrupted file " + file.getName() + ": " + ex2.toString());
                }
            }
        }

        // 2. attempt to read from old file
        final File oldFile = getOldFile(file);
        if (oldFile.exists()) {
            logger.accept("Attempt to read " + oldFile.getName() + ".");
            try {
                byte[] bytes = Files.readAllBytes(oldFile.toPath());
                return Optional.of(reader.read(bytes));
            } catch (Exception ex) {
                logger.accept("Failed to read " + oldFile.getName() + ": " + ex.toString());
            }
        }

        return Optional.empty();
    }

    public static <T> Optional<T> readUTF8(@NotNull final File file, @NotNull final Reader<String, T> reader) {
        return read(file, data -> reader.read(new String(data, StandardCharsets.UTF_8)));
    }

    public static <T> Optional<T> readJSON(@NotNull final File file, @NotNull final Reader<JSONObject, T> reader) {
        return readUTF8(file, text -> reader.read(new JSONObject(text)));
    }

    public static boolean exist(@NotNull File file) {
        return file.exists() || getOldFile(file).exists();
    }

    public static boolean delete(@NotNull final File file) {
        boolean result = true;

        final File oldFile = getOldFile(file);
        if (oldFile.exists()) {
            try {
                Files.delete(oldFile.toPath());
            } catch (Exception ex) {
                result = false;
                logger.accept("Failed to delete " + oldFile.getName() + ": " + ex.toString());
            }
        }

        if (file.exists()) {
            try {
                Files.delete(file.toPath());
            } catch (Exception ex) {
                result = false;
                logger.accept("Failed to delete " + file.getName() + ": " + ex.toString());
            }
        }

        return result;
    }

}
