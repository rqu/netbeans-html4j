/**
 * HTML via Java(tm) Language Bindings
 * Copyright (C) 2013 Jaroslav Tulach <jaroslav.tulach@apidesign.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details. apidesign.org
 * designates this particular file as subject to the
 * "Classpath" exception as provided by apidesign.org
 * in the License file that accompanied this code.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://wiki.apidesign.org/wiki/GPLwithClassPathException
 */
package org.apidesign.html.geo.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import static org.testng.Assert.*;

/**
 *
 * @author Jaroslav Tulach <jtulach@netbeans.org>
 */
final class Compile implements DiagnosticListener<JavaFileObject> {
    private final List<Diagnostic<? extends JavaFileObject>> errors = 
            new ArrayList<Diagnostic<? extends JavaFileObject>>();
    private final Map<String, byte[]> classes;
    private final String pkg;
    private final String cls;
    private final String html;
    private final String sourceLevel;

    private Compile(String html, String code, String sl) throws IOException {
        this.pkg = findPkg(code);
        this.cls = findCls(code);
        this.html = html;
        this.sourceLevel = sl;
        classes = compile(html, code);
    }

    /** Performs compilation of given HTML page and associated Java code
     */
    public static Compile create(String html, String code) throws IOException {
        return create(html, code, "1.7");
    }
    static Compile create(String html, String code, String sourceLevel) throws IOException {
        return new Compile(html, code, sourceLevel);
    }
    
    /** Checks for given class among compiled resources */
    public byte[] get(String res) {
        return classes.get(res);
    }
    
    /** Obtains errors created during compilation.
     */
    public List<Diagnostic<? extends JavaFileObject>> getErrors() {
        List<Diagnostic<? extends JavaFileObject>> err;
        err = new ArrayList<Diagnostic<? extends JavaFileObject>>();
        for (Diagnostic<? extends JavaFileObject> diagnostic : errors) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                err.add(diagnostic);
            }
        }
        return err;
    }
    
    private Map<String, byte[]> compile(final String html, final String code) throws IOException {
        StandardJavaFileManager sjfm = ToolProvider.getSystemJavaCompiler().getStandardFileManager(this, null, null);

        final Map<String, ByteArrayOutputStream> class2BAOS;
        class2BAOS = new HashMap<String, ByteArrayOutputStream>();

        JavaFileObject file = new SimpleJavaFileObject(URI.create("mem://mem"), Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                return code;
            }
        };
        final JavaFileObject htmlFile = new SimpleJavaFileObject(URI.create("mem://mem2"), Kind.OTHER) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                return html;
            }

            @Override
            public InputStream openInputStream() throws IOException {
                return new ByteArrayInputStream(html.getBytes());
            }
        };
        
        final URI scratch;
        try {
            scratch = new URI("mem://mem3");
        } catch (URISyntaxException ex) {
            throw new IOException(ex);
        }
        
        JavaFileManager jfm = new ForwardingJavaFileManager<JavaFileManager>(sjfm) {
            @Override
            public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException {
                if (kind  == Kind.CLASS) {
                    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                    class2BAOS.put(className.replace('.', '/') + ".class", buffer);
                    return new SimpleJavaFileObject(sibling.toUri(), kind) {
                        @Override
                        public OutputStream openOutputStream() throws IOException {
                            return buffer;
                        }
                    };
                }
                
                if (kind == Kind.SOURCE) {
                    final String n = className.replace('.', '/') + ".java";
                    final URI un;
                    try {
                        un = new URI("mem://" + n);
                    } catch (URISyntaxException ex) {
                        throw new IOException(ex);
                    }
                    return new VirtFO(un/*sibling.toUri()*/, kind, n);
                }
                
                throw new IllegalStateException();
            }

            @Override
            public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
                if (location == StandardLocation.SOURCE_PATH) {
                    if (packageName.equals(pkg)) {
                        return htmlFile;
                    }
                }
                
                return null;
            }

            @Override
            public boolean isSameFile(FileObject a, FileObject b) {
                if (a instanceof VirtFO && b instanceof VirtFO) {
                    return ((VirtFO)a).getName().equals(((VirtFO)b).getName());
                }
                
                return super.isSameFile(a, b);
            }

            class VirtFO extends SimpleJavaFileObject {

                private final String n;

                public VirtFO(URI uri, Kind kind, String n) {
                    super(uri, kind);
                    this.n = n;
                }
                private final ByteArrayOutputStream data = new ByteArrayOutputStream();

                @Override
                public OutputStream openOutputStream() throws IOException {
                    return data;
                }

                @Override
                public String getName() {
                    return n;
                }

                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                    data.close();
                    return new String(data.toByteArray());
                }
            }
        };

        ToolProvider.getSystemJavaCompiler().getTask(null, jfm, this, /*XXX:*/Arrays.asList("-source", sourceLevel, "-target", "1.7"), null, Arrays.asList(file)).call();

        Map<String, byte[]> result = new HashMap<String, byte[]>();

        for (Map.Entry<String, ByteArrayOutputStream> e : class2BAOS.entrySet()) {
            result.put(e.getKey(), e.getValue().toByteArray());
        }

        return result;
    }


    @Override
    public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
        errors.add(diagnostic);
    }
    private static String findPkg(String java) throws IOException {
        Pattern p = Pattern.compile("package\\p{javaWhitespace}*([\\p{Alnum}\\.]+)\\p{javaWhitespace}*;", Pattern.MULTILINE);
        Matcher m = p.matcher(java);
        if (!m.find()) {
            throw new IOException("Can't find package declaration in the java file");
        }
        String pkg = m.group(1);
        return pkg;
    }
    private static String findCls(String java) throws IOException {
        Pattern p = Pattern.compile("class\\p{javaWhitespace}*([\\p{Alnum}\\.]+)\\p{javaWhitespace}", Pattern.MULTILINE);
        Matcher m = p.matcher(java);
        if (!m.find()) {
            throw new IOException("Can't find package declaration in the java file");
        }
        String cls = m.group(1);
        return cls;
    }

    String getHtml() {
        String fqn = "'" + pkg + '.' + cls + "'";
        return html.replace("'${fqn}'", fqn);
    }

    void assertErrors() {
        assertFalse(getErrors().isEmpty(), "There are supposed to be some errors");
    }

    void assertError(String expMsg) {
        StringBuilder sb = new StringBuilder();
        sb.append("Can't find ").append(expMsg).append(" among:");
        for (Diagnostic<? extends JavaFileObject> e : errors) {
            String msg = e.getMessage(Locale.US);
            if (msg.contains(expMsg)) {
                return;
            }
            sb.append("\n");
            sb.append(msg);
        }
        fail(sb.toString());
    }
}
