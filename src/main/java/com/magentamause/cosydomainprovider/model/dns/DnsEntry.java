package com.magentamause.cosydomainprovider.model.dns;

import java.util.List;

public record DnsEntry(String name, String type, Long ttl, List<String> values) {
}
