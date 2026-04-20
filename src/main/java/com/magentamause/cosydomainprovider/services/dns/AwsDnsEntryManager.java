package com.magentamause.cosydomainprovider.services.dns;

import com.magentamause.cosydomainprovider.model.dns.DnsEntry;
import java.util.List;

public class AwsDnsEntryManager implements DnsEntryManager {
    @Override
    public void addDnsEntry(String domain, String ip) {}

    @Override
    public List<DnsEntry> getDnsEntries(String domain) {
        return List.of();
    }

    @Override
    public List<DnsEntry> getAllDnsEntries() {
        return List.of();
    }
}
