package com.sshdeploy.deploy.importer;

import com.sshdeploy.deploy.domain.*;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class ActWorkspaceXmlParser implements ActConfigParser {

    private static final String TYPE_DEPLOY = "DEPLOY_HOST_RUN_CONFIGURATION";

    @Override
    public String getParserId() {
        return "act_cloudtoolkit_xml";
    }

    @Override
    public boolean canParse(String content) {
        return content.contains("<cloudToolkitSettings>");
    }

    @Override
    public ImportResult parse(String content) {
        ImportResult result = new ImportResult();
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            Document doc = saxBuilder.build(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
            Element root = doc.getRootElement();

            Map<String, String> profileIdToCredentials = parseProfiles(root.getChild("database"), result);
            Map<String, String> hostIdToServerId = parseHosts(root.getChild("database"), profileIdToCredentials, result);
            Map<String, String> commandContentMap = parseCommands(root.getChild("database"), result);
            parseRunConfigurations(root, hostIdToServerId, commandContentMap, result);

        } catch (Exception e) {
            result.addError("Failed to parse ACT configuration: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    private Map<String, String> parseProfiles(Element database, ImportResult result) {
        Map<String, String> profileIdToCredentials = new HashMap<>();
        if (database == null) return profileIdToCredentials;

        Element profilesElem = database.getChild("profile");
        if (profilesElem == null) return profileIdToCredentials;

        for (Element profileElem : profilesElem.getChildren("profile")) {
            String id = getChildText(profileElem, "id", "");
            String username = getChildText(profileElem, "user", "");
            String password = getChildText(profileElem, "password", "");
            String privateKey = getChildText(profileElem, "privateKey", "");
            String keyphrase = getChildText(profileElem, "keyphrase", "");

            profileIdToCredentials.put(id, username + "::" + password + "::" + privateKey + "::" + keyphrase);
        }
        return profileIdToCredentials;
    }

    private Map<String, String> parseHosts(Element database, Map<String, String> profileIdToCredentials, ImportResult result) {
        Map<String, String> hostIdToServerId = new HashMap<>();
        if (database == null) return hostIdToServerId;

        Element hostsElem = database.getChild("host");
        if (hostsElem == null) return hostIdToServerId;

        Map<String, String> endpointToFirstServerId = new HashMap<>();

        for (Element hostElem : hostsElem.getChildren("host")) {
            String hostId = getChildText(hostElem, "id", "");
            String address = getChildText(hostElem, "address", "");
            String portStr = getChildText(hostElem, "port", "22");
            String profileId = getChildText(hostElem, "profileId", "");
            String remark = getChildText(hostElem, "remark", "");

            if (address.isBlank()) {
                result.addWarning("Skipping host with empty address (id=" + hostId + ")");
                continue;
            }

            int port = parsePort(portStr);
            String endpoint = endpointKey(address, port);
            String duplicateOf = endpointToFirstServerId.get(endpoint);
            if (duplicateOf != null) {
                hostIdToServerId.put(hostId, duplicateOf);
                result.addWarning("Skipped duplicate host " + address + ":" + port + " (same address and port as an earlier entry).");
                continue;
            }

            String credPack = profileIdToCredentials.getOrDefault(profileId, ":::");
            String[] credentials = credPack.split("::", -1);
            String username = credentials.length > 0 ? credentials[0] : "";
            String password = credentials.length > 1 ? credentials[1] : "";
            String privateKey = credentials.length > 2 ? credentials[2] : "";
            String keyphrase = credentials.length > 3 ? credentials[3] : "";

            ServerProfile server = new ServerProfile();
            server.setName(remark.isBlank() ? address : remark);
            server.setHost(address);
            server.setPort(port);
            server.setUsername(username);

            if (!privateKey.isBlank()) {
                server.setAuthType(AuthType.PRIVATE_KEY);
            } else {
                server.setAuthType(AuthType.PASSWORD);
            }

            String proxyId = getChildText(hostElem, "proxyId", "0");
            if (!"0".equals(proxyId) && !proxyId.isBlank()) {
                result.addWarning("Host " + address + " references ACT proxy (proxyId=" + proxyId + "); jump host is not supported and was ignored.");
            }

            server.setDescription("Imported from ACT");

            if (server.getAuthType() == AuthType.PASSWORD) {
                result.putServerImportSecret(server.getId(), password != null ? password : "");
            } else {
                String pk = privateKey != null ? privateKey : "";
                String kp = keyphrase != null ? keyphrase : "";
                result.putServerImportSecret(server.getId(), pk + "::" + kp);
            }

            String serverId = server.getId();
            hostIdToServerId.put(hostId, serverId);
            endpointToFirstServerId.put(endpoint, serverId);
            result.addServer(server);
        }
        return hostIdToServerId;
    }

    private Map<String, String> parseCommands(Element database, ImportResult result) {
        Map<String, String> commandContentMap = new HashMap<>();
        if (database == null) return commandContentMap;

        Element commandsElem = database.getChild("command");
        if (commandsElem == null) return commandContentMap;

        Map<String, String> normalizedContentToCanonicalCommandId = new HashMap<>();

        for (Element commandElem : commandsElem.getChildren("command")) {
            String id = getChildText(commandElem, "id", "");
            String content = commandElem.getChildText("content");
            if (content == null) content = "";

            String commandText = content.trim();
            if (commandText.startsWith("Command:")) {
                int dirIdx = commandText.indexOf(" Directory:");
                if (dirIdx > 0) {
                    commandText = commandText.substring(8, dirIdx).trim();
                } else {
                    commandText = commandText.substring(8).trim();
                }
            }

            commandContentMap.put(id, commandText);

            String norm = normalizeCommandContent(commandText);
            if (normalizedContentToCanonicalCommandId.containsKey(norm)) {
                String canonId = normalizedContentToCanonicalCommandId.get(norm);
                if (!id.isBlank() && canonId != null && !id.equals(canonId)) {
                    result.addCommandIdAlias(id, canonId);
                }
                continue;
            }

            CommandTemplate template = new CommandTemplate();
            if (!id.isBlank()) {
                template.setId(id);
            }
            String label = commandText.isBlank() ? ("ACT " + id) : commandText;
            if (label.length() > 64) {
                label = label.substring(0, 61) + "...";
            }
            template.setName(label);
            template.setContent(commandText);
            template.setExecutionType(CommandExecutionType.AFTER);
            result.addAfterCommand(template);
            normalizedContentToCanonicalCommandId.put(norm, template.getId());
        }
        return commandContentMap;
    }

    private static String endpointKey(String address, int port) {
        return normalizeHost(address) + ":" + port;
    }

    private static String normalizeHost(String host) {
        return host == null ? "" : host.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeCommandContent(String commandText) {
        return commandText == null ? "" : commandText.trim();
    }

    private void parseRunConfigurations(Element root, Map<String, String> hostIdToServerId,
                                        Map<String, String> commandContentMap, ImportResult result) {
        Element ideaSettings = root.getChild("ideaSettings");
        if (ideaSettings == null) {
            result.addWarning("No ideaSettings element found.");
            return;
        }

        String runConfigCData = ideaSettings.getChildText("runConfiguration");
        if (runConfigCData == null || runConfigCData.isBlank()) {
            result.addWarning("No runConfiguration data found.");
            return;
        }

        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            Element configRoot = saxBuilder.build(new ByteArrayInputStream(runConfigCData.getBytes(StandardCharsets.UTF_8))).getRootElement();

            for (Element configElem : configRoot.getChildren("configuration")) {
                String type = configElem.getAttributeValue("type", "");
                if (!TYPE_DEPLOY.equals(type)) {
                    continue;
                }

                // Skip default configurations
                String isDefault = configElem.getAttributeValue("default", "false");
                if ("true".equals(isDefault)) {
                    continue;
                }

                parseDeployConfiguration(configElem, hostIdToServerId, commandContentMap, result);
            }
        } catch (Exception e) {
            result.addWarning("Failed to parse run configurations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void parseDeployConfiguration(Element configElem, Map<String, String> hostIdToServerId,
                                          Map<String, String> commandContentMap, ImportResult result) {
        String name = configElem.getAttributeValue("name", "Imported Deploy");
        String folderName = configElem.getAttributeValue("folderName", "");

        String afterCommand = getOptionValue(configElem, "afterCommand", "");
        String beforeCommand = getOptionValue(configElem, "beforeCommand", "");
        String terminalCommand = getOptionValue(configElem, "terminalCommand", "");
        String location = getOptionValue(configElem, "location", "");
        String pathOrUrl = getOptionValue(configElem, "pathOrUrl", "");
        String uploadType = getOptionValue(configElem, "uploadType", "MAVEN");

        // Parse host IDs from the configuration
        List<String> hostIds = new ArrayList<>();
        Element hostIdsOption = findOptionElement(configElem, "hostIds");
        if (hostIdsOption != null) {
            Element listElem = hostIdsOption.getChild("list");
            if (listElem != null) {
                for (Element optionElem : listElem.getChildren("option")) {
                    String value = optionElem.getAttributeValue("value", "");
                    if (!value.isBlank()) {
                        hostIds.add(value);
                    }
                }
            }
        }

        if (hostIds.isEmpty()) {
            result.addWarning("Skipping deploy config '" + name + "' with no target hosts.");
            return;
        }

        // Build upload config
        UploadConfig upload = new UploadConfig();
        upload.setDirectory(false);
        upload.setLocalPath(pathOrUrl);
        upload.setRemotePath(location);
        result.addUploadConfig(upload);

        // Build deploy profile
        DeployProfile profile = new DeployProfile();
        String fullName = folderName.isBlank() ? name : folderName + " - " + name;
        profile.setName(fullName);
        profile.setUploadConfigRef(upload.getId());

        // Set upload type
        if ("MAVEN".equalsIgnoreCase(uploadType)) {
            profile.setBuildToolType(BuildToolType.MAVEN);
        } else if ("GRADLE".equalsIgnoreCase(uploadType)) {
            profile.setBuildToolType(BuildToolType.GRADLE);
        } else {
            profile.setBuildToolType(BuildToolType.NONE);
        }

        // Parse before command
        if (!beforeCommand.isBlank()) {
            CommandTemplate beforeCmd = new CommandTemplate();
            beforeCmd.setName("Before Command (" + name + ")");
            beforeCmd.setContent(beforeCommand);
            beforeCmd.setExecutionType(CommandExecutionType.BEFORE);
            profile.getBeforeCommandRefs().add(beforeCmd.getId());
            result.addBeforeCommand(beforeCmd);
        }

        // Parse after command
        if (!afterCommand.isBlank()) {
            CommandTemplate afterCmd = new CommandTemplate();
            afterCmd.setName("After Command (" + name + ")");
            afterCmd.setContent(afterCommand);
            afterCmd.setExecutionType(CommandExecutionType.AFTER);
            profile.getAfterCommandRefs().add(afterCmd.getId());
            result.addAfterCommand(afterCmd);
        }

        // Set terminal command
        if (!terminalCommand.isBlank()) {
            profile.setTerminalCommand(terminalCommand);
            profile.setTerminalCommandEnabled(true);
        }

        // Create one profile per host ID
        for (String hostId : hostIds) {
            String serverId = hostIdToServerId.get(hostId);
            if (serverId == null) {
                result.addWarning("Deploy config '" + name + "' references unknown host id: " + hostId);
                continue;
            }

            DeployProfile hostProfile = new DeployProfile();
            hostProfile.setName(fullName);
            hostProfile.setServerRef(serverId);
            hostProfile.setUploadConfigRef(upload.getId());
            hostProfile.setBuildToolType(profile.getBuildToolType());
            hostProfile.setTerminalCommand(profile.getTerminalCommand());
            hostProfile.setTerminalCommandEnabled(profile.isTerminalCommandEnabled());

            for (String cmdRef : profile.getBeforeCommandRefs()) {
                hostProfile.getBeforeCommandRefs().add(cmdRef);
            }
            for (String cmdRef : profile.getAfterCommandRefs()) {
                hostProfile.getAfterCommandRefs().add(cmdRef);
            }

            result.addDeployProfile(hostProfile);
        }
    }

    private Element findOptionElement(Element configElem, String optionName) {
        for (Element option : configElem.getChildren("option")) {
            if (optionName.equals(option.getAttributeValue("name"))) {
                return option;
            }
        }
        return null;
    }

    private String getOptionValue(Element configElem, String optionName, String defaultValue) {
        Element option = findOptionElement(configElem, optionName);
        if (option == null) return defaultValue;
        String value = option.getAttributeValue("value");
        return value == null ? defaultValue : value.trim();
    }

    private String getChildText(Element parent, String childName, String defaultValue) {
        Element child = parent.getChild(childName);
        if (child == null) return defaultValue;
        String text = child.getTextTrim();
        return text == null ? defaultValue : text;
    }

    private int parsePort(String portStr) {
        try {
            return Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            return 22;
        }
    }
}
