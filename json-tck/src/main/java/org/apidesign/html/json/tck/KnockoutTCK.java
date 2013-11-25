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
package org.apidesign.html.json.tck;

import java.net.URI;
import java.util.Map;
import net.java.html.BrwsrCtx;
import net.java.html.json.tests.ConvertTypesTest;
import net.java.html.json.tests.KnockoutTest;
import net.java.html.json.tests.JSONTest;
import net.java.html.json.tests.OperationsTest;
import net.java.html.json.tests.WebSocketTest;
import org.openide.util.lookup.ServiceProvider;

/** Entry point for providers of different HTML binding technologies (like
 * Knockout.js in JavaFX's WebView). Sample usage:
 * <pre>
{@link ServiceProvider @ServiceProvider}(service = KnockoutTCK.class)
public final class MyKnockoutBindingTest extends KnockoutTCK {
    {@link Override @Override}
    protected BrwsrCtx createContext() {
        // use {@link ContextBuilder}.{@link ContextBuilder#build() build}();
    }

    {@code @Factory} public static Object[] create() {
        return VMTest.newTests().withClasses({@link KnockoutTCK#testClasses}()).build();
    }
}
 * </pre>
 *
 * @author Jaroslav Tulach <jtulach@netbeans.org>
 */
public abstract class KnockoutTCK {
    protected KnockoutTCK() {
    }
    
    /** Implement to create new context for the test. 
     * Use {@link ContextBuilder} to implement context for your technology.
     */
    public abstract BrwsrCtx createContext();
    
    /** Create a JSON object as seen by the technology
     * @param values mapping from names to values of properties
     */
    public abstract Object createJSON(Map<String,Object> values);

    /** Executes script in the context of current window
     * 
     * @param script the JavaScript code to execute
     * @param arguments arguments sent to the script (can be referenced as <code>arguments[0]</code>)
     * @return the output of the execution
     */
    public abstract Object executeScript(String script, Object[] arguments);

    /** Creates a URL which later returns content with given
     * <code>mimeType</code> and <code>content</code>. The 
     * content may be processed by the provided <code>parameters</code>.
     * 
     * @param content
     * @param mimeType
     * @param parameters
     * @return 
     */
    public abstract URI prepareURL(String content, String mimeType, String[] parameters);
    
    /** Gives you list of classes included in the TCK. Their test methods
     * are annotated by {@link KOTest} annotation. The methods are public
     * instance methods that take no arguments.
     * 
     * @return classes with methods annotated by {@link KOTest} annotation
     */
    protected static Class<?>[] testClasses() {
        return new Class[] { 
            ConvertTypesTest.class,
            JSONTest.class,
            KnockoutTest.class,
            OperationsTest.class,
            WebSocketTest.class
        };
    }

    /** Some implementations cannot fully support web sockets and fail.
     * 
     * @return true, if UnsupportedOperationException reported from a web
     *    socket open operation is acceptable reply
     */
    public boolean canFailWebSocketTest() {
        return false;
    }


}
