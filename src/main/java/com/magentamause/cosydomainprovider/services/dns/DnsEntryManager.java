package com.magentamause.cosydomainprovider.services.dns;

import com.magentamause.cosydomainprovider.model.dns.DnsEntry;

import java.util.List;

public interface DnsEntryManager {
    void addDnsEntry(String domain, String ip);

    List<DnsEntry> getDnsEntries(String domain);

    List<DnsEntry> getAllDnsEntries();
}
