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
package net.java.html.js.tests;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import org.netbeans.html.json.tck.KOTest;

/**
 *
 * @author Jaroslav Tulach
 */
public class GCBodyTest {
    Reference<?> ref;
    
    @KOTest public void callbackWithParameters() throws InterruptedException {
        if (ref != null) {
            assertGC(ref, "Can disappear!");
            return;
        }
        Sum s = new Sum();
        int res = Bodies.sumIndirect(s);
        assert res == 42 : "Expecting 42";
        Reference<?> ref = new WeakReference<Object>(s);
        s = null;
        assertGC(ref, "Can disappear!");
    }
    
    @KOTest public void holdObjectAndReleaseObject() throws InterruptedException {
        if (ref != null) {
            assertGC(ref, "Can disappear!");
            return;
        }
        Sum s = new Sum();
        Object obj = Bodies.instance(0);
        Bodies.setX(obj, s);
        
        ref = new WeakReference<Object>(s);
        s = null;
        assertNotGC(ref, "Cannot disappear!");
        
        Bodies.setX(obj, null);
        
        assertGC(ref, "Can disappear!");
    }
    
    private static void assertGC(Reference<?> ref, String msg) throws InterruptedException {
        for (int i = 0; i < 25; i++) {
            if (ref.get() == null) {
                return;
            }
            int size = Bodies.gc(Math.pow(2.0, i));
            try {
                System.gc();
                System.runFinalization();
            } catch (Error err) {
                err.printStackTrace();
            }
        }
        throw new InterruptedException(msg);
    }
    
    private static void assertNotGC(Reference<?> ref, String msg) throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            if (ref.get() == null) {
                throw new IllegalStateException(msg);
            }
            int size = Bodies.gc(Math.pow(2.0, i));
            try {
                System.gc();
                System.runFinalization();
            } catch (Error err) {
                err.printStackTrace();
            }
        }
    }
}
