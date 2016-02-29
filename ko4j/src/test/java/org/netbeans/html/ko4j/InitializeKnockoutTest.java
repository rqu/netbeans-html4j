/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013-2014 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Oracle. Portions Copyright 2013-2014 Oracle. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package org.netbeans.html.ko4j;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import net.java.html.BrwsrCtx;
import net.java.html.boot.fx.FXBrowsers;
import net.java.html.js.JavaScriptBody;
import net.java.html.json.Models;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertNotNull;

/**
 *
 * @author Jaroslav Tulach
 */
public class InitializeKnockoutTest {
    public InitializeKnockoutTest() {
    }
    
    @BeforeClass
    public void initFX() throws Throwable {
        new Thread("initFX") {
            @Override
            public void run() {
                if (Platform.isFxApplicationThread()) {
                    new App().start(new Stage());
                } else {
                    try {
                        App.launch(App.class);
                    } catch (IllegalStateException ex) {
                        Platform.runLater(this);
                    }
                }
            }
        }.start();
        App.CDL.await();
    }

    @JavaScriptBody(args = {}, body = "return typeof ko !== 'undefined' ? ko : null;")
    static native Object ko();
    
    @Test
    public void brwsrCtxExecute() throws Throwable {
        final CountDownLatch init = new CountDownLatch(1);
        final BrwsrCtx[] ctx = { null };
        FXBrowsers.runInBrowser(App.webView(), new Runnable() {
            @Override
            public void run() {
                ctx[0] = BrwsrCtx.findDefault(InitializeKnockoutTest.class);
                init.countDown();
            }
        });
        init.await();

        final CountDownLatch cdl = new CountDownLatch(1);
        FXBrowsers.runInBrowser(App.webView(), new Runnable() {
            @Override
            public void run() {
                assertNull(ko(), "Knockout isn't yet defined");
                Models.toRaw(null);
                assertNotNull(ko(), "After call to toRaw, ko is defined");

                cdl.countDown();
            }
        });

        cdl.await();
    }

    public static class App extends Application {
        static final CountDownLatch CDL = new CountDownLatch(1);
        private static BorderPane pane;

        static WebView webView() {
            try {
                CDL.await();
            } catch (InterruptedException ex) {
                throw new IllegalStateException(ex);
            }
            return (WebView)System.getProperties().get("v1");
        }

        @Override
        public void start(Stage stage) {
            pane= new BorderPane();
            Scene scene = new Scene(pane, 800, 600);
            stage.setScene(scene);
            final WebView w1 = new WebView();
            System.getProperties().put("v1", w1);
            pane.setCenter(w1);

            
            URL url = InitializeKnockoutTest.class.getResource("test.html");
            assertNotNull(url);
            FXBrowsers.load(w1, url, new Runnable() {
                @Override
                public void run() {
                    CDL.countDown();
                }
            });

        }
        
        
    }
}
