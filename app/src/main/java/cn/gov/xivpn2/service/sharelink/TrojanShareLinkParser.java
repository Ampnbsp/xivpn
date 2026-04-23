package cn.gov.xivpn2.service.sharelink;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.gov.xivpn2.database.Proxy;
import cn.gov.xivpn2.service.SubscriptionWork;
import cn.gov.xivpn2.xrayconfig.Outbound;
import cn.gov.xivpn2.xrayconfig.StreamSettings;
import cn.gov.xivpn2.xrayconfig.TrojanServerSettings;
import cn.gov.xivpn2.xrayconfig.TrojanSettings;

/**
 * VLESS share link parser.
 * <p>
 * Format: {@code vless://uuid@host:port?params#label}
 */
public class TrojanShareLinkParser extends BaseVMessVLessParser {


    @Override
    public Proxy parse(String uri) throws BadShareLinkException {
        if (!uri.startsWith("trojan://")) {
            throw new IllegalArgumentException("not a trojan URI: " + uri);
        }

        try {

            URI parsed = new URI(uri);

            Proxy proxy = new Proxy();
            proxy.label = URLDecoder.decode(nullable(parsed.getFragment(), "TROJAN"), "UTF-8");
            proxy.protocol = "trojan";

            Map<String, String> query = splitQuery(parsed.getRawQuery());

            // protocol settings
            Outbound<TrojanSettings> outbound = new Outbound<>();
            outbound.protocol = "trojan";
            outbound.settings = new TrojanSettings();
            outbound.settings.servers = new ArrayList<>();

            TrojanServerSettings server = new TrojanServerSettings();
            server.address = parsed.getHost();
            server.port = parsed.getPort();
            server.password = parsed.getUserInfo();

            //VlessUser vlessUser = new VlessUser();
            //vlessUser.id = nullable(parsed.getUserInfo(), "");
            //vlessUser.encryption = query.getOrDefault("encryption", "none");
            //vlessUser.flow = nullable(query.get("flow"), "");

            //server.users.add(vlessUser);
            outbound.settings.servers.add(server);

            // stream settings (transport + security)
            outbound.streamSettings = parseStreamSettings(query, parsed.getHost());

            proxy.config = new Gson().toJson(outbound);
            return proxy;
        } catch (Exception e) {
            throw new BadShareLinkException(e);
        }

    }

    @Override
    public String marshal(Proxy proxy) throws MarshalProxyException {
        Outbound<TrojanSettings> outbound = new Gson().fromJson(proxy.config, new TypeToken<>() {});

        TrojanServerSettings server = outbound.settings.servers.get(0);
        //VlessUser vlessUser = server.users.get(0);
        StreamSettings streamSettings = outbound.streamSettings;

        // Build query params from stream settings
        Map<String, String> streamQueries = marshalStreamSettingsQueries(streamSettings);

        // Build final ordered query map
        Map<String, String> queries = new LinkedHashMap<>();

        // transport type first
        queries.put("type", streamQueries.remove("type"));

        // transport-specific params next
        for (String key : new String[]{"host", "path", "serviceName", "mode", "authority", "extra"}) {
            if (streamQueries.containsKey(key)) {
                queries.put(key, streamQueries.remove(key));
            }
        }

        // VLESS-specific: encryption (omit if "none" per spec)
        //String encryption = nullable(vlessUser.encryption, "none");
        //if (!encryption.equals("none")) {
        //    queries.put("encryption", encryption);
        //}

        // VLESS-specific: flow
        //queries.put("flow", nullable(vlessUser.flow, ""));

        // security + security-specific params
        queries.putAll(streamQueries);

        return "trojan://"
                + server.password
                + "@" + quoteHost(server.address) + ":" + server.port
                + queryFromMap(queries)
                + "#" + quote(proxy.label);
    }
}
