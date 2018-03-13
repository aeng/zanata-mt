/*
 * Copyright 2018, Red Hat, Inc. and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.zanata.magpie.action;

import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.ServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 **/
public class FrontendAssetsTest {

    @Test
    public void testInit() throws Exception {
        String frontendCss = "frontend.css";
        String frontendJs = "frontend.js";
        String vendorJs = "vendor.js";
        String contextPath = "mt";

        ServletContext context = Mockito.mock(ServletContext.class);
        when(context.getContextPath()).thenReturn(contextPath);

        FrontendManifest manifest = new FrontendManifest();
        manifest.setFrontendCss(frontendCss);
        manifest.setFrontendJs(frontendJs);
        manifest.setVendorJs(vendorJs);
        FrontendAssetsMock assets = new FrontendAssetsMock(manifest);
        assets.onInit(context);

        assertThat(assets.getFrontendCss()).isEqualTo(contextPath + "/" + frontendCss);
        assertThat(assets.getFrontendJs()).isEqualTo(contextPath + "/" + frontendJs);
        assertThat(assets.getVendorJs()).isEqualTo(contextPath + "/" + vendorJs);
    }


    class FrontendAssetsMock extends FrontendAssets {
        private FrontendManifest mockManifest;
        public FrontendAssetsMock(FrontendManifest mockManifest) throws Exception {
            this.mockManifest = mockManifest;
        }

        @Override
        protected FrontendManifest getManifest() throws Exception {
            return mockManifest;
        }
    }

}
