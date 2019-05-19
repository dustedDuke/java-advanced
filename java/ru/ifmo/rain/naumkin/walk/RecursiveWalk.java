package ru.ifmo.rain.naumkin.walk;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class RecursiveWalk {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Wrong input.");
        } else {
            RecursiveWalk rwalk = new RecursiveWalk();
            rwalk.start(args[0], args[1]);
        }
    }
    
    public class CustomFileVisitor extends SimpleFileVisitor<Path> {

        private BufferedWriter output;

        CustomFileVisitor(BufferedWriter writer) {
            output = writer;
        }

        private FileVisitResult writeToFile(String contents) {
            try {
                output.write(contents);
                output.newLine();
                return FileVisitResult.CONTINUE;
            } catch (IOException e) {
                System.out.println(e.getMessage());
                return FileVisitResult.TERMINATE;
            }
        }

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes fileAttributes) {
            return writeToFile(String.format("%08x %s", RecursiveWalk.getHash(path), path.toString()));
        }

        @Override
        public FileVisitResult visitFileFailed(Path path, IOException e) {
            return writeToFile("00000000 " + path.toString());
        }
    }


    private void start(String inputPath, String outputPath) {
        try {
            Path input = Paths.get(inputPath);
            Path output = Paths.get(outputPath);

            if(output.getParent() != null && Files.exists(output.getParent())) {
                Files.createDirectories(output.getParent());
            }

            try(BufferedReader reader = Files.newBufferedReader(input);
                BufferedWriter writer = Files.newBufferedWriter(output)) {


                String dir;
                CustomFileVisitor visitor = new CustomFileVisitor(writer);
                while ((dir = reader.readLine()) != null) {
                    try {
                        Files.walkFileTree(Paths.get(dir), visitor);
                    } catch (InvalidPathException e) {
                        writer.write("00000000 " + dir);
                        writer.newLine();
                    }
                }

            } catch (IOException e) {
                System.out.println("Visitor exception " + e.getMessage());
            }

        } catch (IOException | InvalidPathException e) {
            System.out.println("Path resolving exception " + e.getMessage());
        }
    }

    private static final int FNV1 = 0x01000193;
    private static final int FNV2 = 0x811c9dc5;

    private static final int BUF_SIZE = 4096;

    public static int getHash(Path path) {
        int hash = FNV2;

        try (FileInputStream inputStream = new FileInputStream(path.toString())) {
            int read;
            byte[] buf = new byte[BUF_SIZE];
            while ((read = inputStream.read(buf)) != -1) {
                for (int i = 0; i < read; i++) {
                    hash *= FNV1;
                    hash ^= buf[i] & 0xff;
                }
            }
        } catch (IOException e) {
            hash = 0;
        }
        return hash;
    }
}



