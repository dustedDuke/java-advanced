package ru.ifmo.rain.naumkin.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.IntFunction;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;

/**
 * Implementor class generates implementation of passed classes/interfaces and creates a .jar file, containing these
 * classes.
 *
 * @author Dmitry Naumkin
 * @version 1.0
 *
 */

public class Implementor implements Impler, JarImpler {
    /**
     * Type of class which will be implemented
     */
    private Class<?> type;
    /**
     * Contains available constructor
     */
    private Constructor availableConstructor;
    /**
     * Contains old name + "Impl"
     */
    private String newName;
    /**
     * Instance of inner class, that cleans directories
     */
    private static final FileVisitor<Path> DIRECTORY_CLEANER = new Cleaner();
    /**
     * Contains package path
     */
    private Path packagePath;
    /**
     * Contains package name
     */
    private String packageName;


    /**
     * Helper class, that cleans compiled classes during creation of .jar file
     *
     */
    private static class Cleaner extends SimpleFileVisitor<Path> {

        /**
         *
         * @param file path of file for deletion
         * @param attrs file attributes
         * @return result of deletion
         * @throws IOException
         */
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        /**
         *
         * @param dir directory for deletion
         * @param exc exception instance
         * @return result of deletion
         * @throws IOException
         */
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    }

    /**
     * Writer wrapper for Unicode formatting
     */
    private static class UnicodeWriter extends FilterWriter {

        /**
         * Constructor
         * @param writer
         */
        UnicodeWriter(Writer writer) {
            super(writer);
        }

        /**
         * Writes character
         * @param i
         * @throws IOException
         */
        @Override
        public void write(int i) throws IOException {
            if (i >= 128) {
                out.write(String.format("\\u%04X", i));
            } else {
                out.write(i);
            }
        }

        /**
         * Writes shifted string part
         * @param s
         * @param offset
         * @param length
         * @throws IOException
         */
        @Override
        public void write(String s, int offset, int length) throws IOException {
            for (int i = offset; i < offset + length; i++) {
                write(s.charAt(i));
            }
        }

        /**
         * Simply writes string
         * @param str
         * @throws IOException
         */
        @Override
        public void write(String str) throws IOException {
            write(str, 0, str.length());
        }
    }


    /**
     * Wrapper class for {@link java.lang.reflect.Method} for storing them in {@link java.util.HashSet}.
     * Overloads {@link MethodWrapper#hashCode()} and {@link MethodWrapper#equals(Object)} methods to store methods
     * properly.
     */
    private static class MethodWrapper {
        private final Method method;

        /**
         * Constructor
         */
        MethodWrapper(Method method) {
            this.method = method;
        }

        /**
         * Checks if signatures of methods are equal
         * @param obj
         * @return
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof MethodWrapper)) {
                return false;
            }
            MethodWrapper otherWrapper = (MethodWrapper) obj;
            return method.getName().equals(otherWrapper.method.getName()) &&
                    Arrays.equals(method.getParameterTypes(), otherWrapper.method.getParameterTypes());
        }


        /**
         * Hash code of method based on signature
         * @return hash code
         */
        @Override
        public int hashCode() {
            return Arrays.hashCode(method.getParameterTypes()) * 1024 +
                    method.getName().hashCode();
        }

        /**
         * Returns {@link Method} instance
         * @return inner method
         */
        Method getMethod() {
            return method;
        }
    }

    /**
     * Default implementor constructor
     */
    public Implementor() {}

    /**
     * Main method, that can create both classes and .jar file, depending on argument values:
     * <ul>
     *     <li> Uses {@link Implementor#implement(Class, Path)} </li>
     *     <li> Uses {@link Implementor#implementJar(Class, Path)} </li>
     * </ul>
     *
     * @param args arguments (mode, class name, filepath)
     */
    public static void main(String[] args) {
        if (args == null || args.length < 2 || args.length > 3) {
            System.out.println("Wrong input.");
            return;
        }
        Implementor implementor = new Implementor();
        String className;
        String rootPath;

        try {
            if (args[0].equals("-jar")) {
                className = args[1];
                rootPath = args[2];
                implementor.implementJar(Class.forName(className), Paths.get(rootPath));

            } else {
                className = args[0];
                rootPath = args[1];
                implementor.implement(Class.forName(className), Paths.get(rootPath));
            }
        } catch (ClassNotFoundException | InvalidPathException | ImplerException e) {
            System.out.println(e.getMessage());
        }
    }


    /**
     * Generates class/interface source code
     * First, checks if type of passed token is implementable, and gets any available constructor
     * Then, creates directories for storing generated classes, if they don't exist
     * After that, writes file with BufferedWriter, using other methods to generate separate parts of class
     * <ul>
     *     <li>{@link Implementor#generateArgument(int, IntFunction)} (Method)}</li>
     *     <li>{@link Implementor#generateFullArguments(Parameter[])} (Method)}</li>
     *     <li>{@link Implementor#generateDeclaration(Executable)} (Method)}</li>
     *     <li>{@link Implementor#generateConstructor(Constructor, String)} (Method)}</li>
     *     <li>{@link Implementor#generateExceptions(Class[])} (Method)}</li>
     *     <li>{@link Implementor#generateMethod(Method)} (Method)}</li>
     *     <li>{@link Implementor#generateMethods()}</li>
     *     <li>{@link Implementor#fetchSuperAbstract()}</li>
     * </ul>
     * @param type type of creating instance
     * @param root root directory.
     * @throws ImplerException
     */
    @Override
    public void implement(Class<?> type, Path root) throws ImplerException {

        this.type = type;

        if (type.isPrimitive() || type.isArray() || type.equals(Enum.class) || Modifier.isFinal(type.getModifiers())) {
            throw new ImplerException("Wrong type");
        } else if (!type.isInterface()) {
            Optional<Constructor<?>> constructor = Arrays.stream(type.getDeclaredConstructors())
                                                    .filter(c -> !Modifier.isPrivate(c.getModifiers()))
                                                    .findAny();
            if (constructor.isPresent()) {
                availableConstructor = constructor.get();
            } else {
                throw new ImplerException("Not interface and no constructors");
            }
        }


        packagePath = root;
        packageName = "";
        if(type.getPackage() != null) {
            packagePath = root.resolve(type.getPackage().getName().replace(".", File.separator));
            packageName = "package " + type.getPackage().getName() + ";\n";
        }

        try {
            Files.createDirectories(packagePath);
        } catch (IOException e) {
            throw new ImplerException(e.getMessage());
        }

        newName = type.getSimpleName() + "Impl";
        Path filePath = packagePath.resolve(newName + ".java");

        try (Writer writer = new UnicodeWriter(Files.newBufferedWriter(filePath))) {

            writer.write(packageName);
            writer.write("public class " + newName + " ");
            if (type.isInterface()) {
                writer.write("implements ");
            } else {
                writer.write("extends ");;
            }
            writer.write(type.getName() + " {\n");
            if (availableConstructor != null) {
                writer.write(generateConstructor(availableConstructor, newName));
            } else {
                assert (type.isInterface());
            }
            writer.write(generateMethods());
            writer.write("}");
        } catch (IOException e) {
            throw new ImplerException("File creation failed " + e.getMessage());
        }
    }

    /**
     * Generates single argument
     * @param length argument length
     * @param mapper mapper
     * @return
     */
    private String generateArgument(int length, IntFunction<String> mapper) {
        return IntStream
                .range(0, length)
                .mapToObj(mapper)
                .collect(Collectors.joining(" ,"));
    }

    /**
     * Generate all arguments using {@link Implementor#generateArgument(int, IntFunction)}
     * @param parameters
     * @return
     */
    private String generateFullArguments(Parameter[] parameters) {
        return generateArgument(
                parameters.length,
                i -> parameters[i].getType().getCanonicalName() + " arg" + i
        );
    }

    /**
     * Generates exceptions or returns empty string
     * @param exceptions
     * @return
     */
    private String generateExceptions(Class[] exceptions) {
        if (exceptions.length == 0) {
            return "";
        }
        return "throws " + Arrays.stream(exceptions)
                .map(Class::getCanonicalName)
                .collect(Collectors.joining(", "));
    }


    /**
     * Generates method declaration using
     * {@link Implementor#generateFullArguments(Parameter[])}
     * {@link Implementor#generateExceptions(Class[])}
     * @param executable
     * @return
     */
    private String generateDeclaration(Executable executable) {
        return "(" + generateFullArguments(executable.getParameters()) + ")" +
                generateExceptions(executable.getExceptionTypes()) + " {\n        ";
    }

    /**
     * Generates public constructor using super constructor
     * @param constructor super constructor
     * @param className name of class
     * @return
     */
    private String generateConstructor(Constructor constructor, String className) {
        String header = "    public " + className + generateDeclaration(constructor) + "super(";
        String superArguments = generateArgument(constructor.getParameterCount(), i -> "arg" + i);
        String body = superArguments + ");\n    }\n\n";
        return header + body;
    }


    /**
     * Helper method for adding abstract super methods to set
     * @param methods
     * @param set
     */
    private void addMethodsToSet(Method[] methods, Set<MethodWrapper> set) {
        Arrays.stream(methods)
                .filter(method -> Modifier.isAbstract(method.getModifiers()))
                .map(MethodWrapper::new)
                .forEach(set::add);
    }


    /**
     * Gets abstract methods from super
     * @return
     */
    private Set<MethodWrapper> fetchSuperAbstract() {
        Set<MethodWrapper> res = new HashSet<>();
        Class<?> cur = type;
        while (cur != null) {
            addMethodsToSet(cur.getDeclaredMethods(), res);
            cur = cur.getSuperclass();
        }
        return res;
    }


    /**
     * Single method generation
     * @param method
     * @return
     */
    private String generateMethod(Method method) {
        Class resultType = method.getReturnType();
        String modifiers = Modifier.toString(
                method.getModifiers()
                        & ~Modifier.ABSTRACT
                        & ~Modifier.TRANSIENT
                        & ~Modifier.NATIVE
        );
        String res = "    " + modifiers + " " + resultType.getCanonicalName() + " " + method.getName() +
                generateDeclaration(method) + "return";

        String returnValue;
        if (resultType.equals(void.class)) {
            returnValue = ";";
        } else if (resultType.equals(boolean.class)) {
            returnValue = " false;";
        } else if (resultType.isPrimitive()) {
            returnValue = " 0;";
        } else {
            returnValue = " null;";
        }
        return res + returnValue + "\n    }\n\n";
    }

    /**
     * Uses {@link Implementor#generateMethods()} to generate bunch of methods
     * @return
     */
    private String generateMethods() {
        Set<MethodWrapper> methods = fetchSuperAbstract();
        addMethodsToSet(type.getMethods(), methods);

        return methods.stream()
                .map(methodWrapper -> generateMethod(methodWrapper.getMethod()))
                .collect(Collectors.joining());
    }


    /**
     * Compiles implementation to .class for further usage in .jar creation
     * @param token
     * @param pathToTempDirectory
     * @return
     * @throws ImplerException
     */
    private Path implementAndCompile(Class<?> token, Path pathToTempDirectory) throws ImplerException {
        implement(token, pathToTempDirectory);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Compiler error");
        }

        pathToTempDirectory = pathToTempDirectory.resolve(token.getPackage().getName().replace(".", File.separator));

        int compileResult = compiler.run(null, null, null,
                pathToTempDirectory.resolve(newName + ".java").toString(),
                "-cp",
                pathToTempDirectory.toString() + File.pathSeparator
                        + System.getProperty("java.class.path")
        );
        if (compileResult != 0) {
            throw new ImplerException("return " + compileResult);
        }
        return pathToTempDirectory.resolve(newName + ".class");
    }


    /**
     * Creates .jar file
     * @param token
     * @param jarFile
     * @param pathToClassFile
     * @throws IOException
     */
    private void createJarFile(Class<?> token, Path jarFile, Path pathToClassFile) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            String fullClassName = token.getCanonicalName().replace(".", "/") + "Impl.class";
            out.putNextEntry(new ZipEntry(fullClassName));
            Files.copy(pathToClassFile, out);
            out.closeEntry();
        }
    }


    /**
     * Second mode of class, that generates .jar file using
     * {@link Implementor#createJarFile(Class, Path, Path)}
     * @param token type token to create implementation for.
     * @param jarFile target <tt>.jar</tt> file.
     * @throws ImplerException
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        Objects.requireNonNull(token);
        Objects.requireNonNull(jarFile);

        this.type = token;
        Path temporaryDir;
        try {
            temporaryDir = Files.createTempDirectory(jarFile.toAbsolutePath().getParent(), "tmp");
        } catch (IOException e) {
            throw new ImplerException(e.getMessage());
        }

        try {
            Path pathToClassFile = implementAndCompile(token, temporaryDir);
            createJarFile(token, jarFile, pathToClassFile);
        } catch (IOException e) {
            throw new ImplerException(e.getMessage());
        } finally {
            try {
                Files.walkFileTree(temporaryDir, DIRECTORY_CLEANER);
            } catch (IOException e) {
                System.err.println(temporaryDir);
            }
        }
    }
}