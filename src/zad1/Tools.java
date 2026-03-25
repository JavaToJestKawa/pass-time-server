package zad1;


import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Tools {
    static Options createOptionsFromYaml(String fileName) throws Exception {
        String yaml = Files.readAllLines(Paths.get(fileName))
                .stream()
                .collect(Collectors.joining(System.lineSeparator()));

        Map<String, Object> map = new Yaml().loadAs(yaml, Map.class);
        String host = (String) map.get("host");
        int port = (int) map.get("port");
        boolean concurMode = (boolean) map.get("concurMode");
        boolean showSendRes = (boolean) map.get("showSendRes");
        Map<String, List<String>> clientsMap = (Map<String, List<String>>) map.get("clientsMap");

        return new Options(host, port, concurMode, showSendRes, clientsMap);
    }
}
