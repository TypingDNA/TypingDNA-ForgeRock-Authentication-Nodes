/*
  Copyright 2020 TypingDNA Inc. (https://www.typingdna.com)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/


package com.typingdna.nodes;

import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.forgerock.openam.auth.node.api.AbstractNodeAmPlugin;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.plugins.PluginException;


/**
 * Definition of an <a href="https://backstage.forgerock.com/docs/am/6/apidocs/org/forgerock/openam/auth/node/api/AbstractNodeAmPlugin.html">AbstractNodeAmPlugin</a>.
 * Implementations can use {@code @Inject} setters to get access to APIs
 * available via Guice dependency injection. For example, if you want to add an SMS service on install, you
 * can add the following setter:
 * <pre><code>
 * {@code @Inject}
 * public void setPluginTools(PluginTools tools) {
 *     this.tools = tools;
 * }
 * </code></pre>
 * So that you can use the addSmsService api to load your schema XML for example.
 * PluginTools javadoc may be found
 * <a href="https://backstage.forgerock.com/docs/am/6/apidocs/org/forgerock/openam/plugins/PluginTools.html#addSmsService-java.io.InputStream-">here</a>
 * <p>
 * It can be assumed that when running, implementations of this class will be singleton instances.
 * </p>
 * <p>
 * It should <i>not</i> be expected that the runtime singleton instances will be the instances on which
 * {@link #onAmUpgrade(String, String)} will be called. Guice-injected properties will also <i>not</i> be populated
 * during that method call.
 * </p>
 * <p>
 * Plugins should <i>not</i> use the {@code ShutdownManager}/{@code ShutdownListener} API for handling shutdown, as
 * the order of calling those listeners is not deterministic. The {@link #onShutdown()} method for all plugins will
 * be called in the reverse order from the order that {@link #onStartup()} was called, with dependent plugins being
 * notified after their dependencies for startup, and before them for shutdown.
 * </p>
 *
 * @since AM 5.5.0
 */
public class TypingDNAPlugin extends AbstractNodeAmPlugin {

    static private final String currentVersion = "1.8.2";

    public TypingDNAPlugin() {
    }

    /**
     * Specify the Map of list of node classes that the plugin is providing. These will then be installed and
     * registered at the appropriate times in plugin lifecycle.
     *
     * @return The list of node classes.
     */
    @Override
    protected Map<String, Iterable<? extends Class<? extends Node>>> getNodesByVersion() {
        return ImmutableMap.of(
                TypingDNAPlugin.currentVersion, Arrays.asList(
                        TypingDNADecisionNode.class,
                        TypingDNARecorder.class,
                        TypingDNAShortPhraseCollector.class,
                        TypingDNAResetProfile.class
                ));
    }

    /**
     * Handle plugin installation. This method will only be called once, on first AM startup once the plugin
     * is included in the classpath. The {@link #onStartup()} method will be called after this one.
     * <p>
     * No need to implement this unless your AuthNode has specific requirements on install.
     */
    @Override
    public void onInstall() throws PluginException {
        super.onInstall();
    }

    /**
     * This method will be called when the version returned by {@link #getPluginVersion()} is higher than the
     * version already installed. This method will be called before the {@link #onStartup()} method.
     * <p>
     * No need to implement this untils there are multiple versions of your auth node.
     *
     * @param fromVersion The old version of the plugin that has been installed.
     */
    @Override
    public void upgrade(String fromVersion) throws PluginException {
        super.upgrade(fromVersion);
        pluginTools.upgradeAuthNode(TypingDNARecorder.class);
        pluginTools.upgradeAuthNode(TypingDNAShortPhraseCollector.class);
        pluginTools.upgradeAuthNode(TypingDNADecisionNode.class);
        pluginTools.upgradeAuthNode(TypingDNAResetProfile.class);
    }

    /**
     * The plugin version. This must be in semver (semantic version) format.
     *
     * @return The version of the plugin.
     * @see <a href="https://www.osgi.org/wp-content/uploads/SemanticVersioning.pdf">Semantic Versioning</a>
     */
    @Override
    public String getPluginVersion() {
        return TypingDNAPlugin.currentVersion;
    }
}
