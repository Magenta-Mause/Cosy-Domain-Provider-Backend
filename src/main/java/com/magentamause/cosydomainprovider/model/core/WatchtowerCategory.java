package com.magentamause.cosydomainprovider.model.core;

/** What the nightly Watchtower agent concluded a subdomain is being used for. */
public enum WatchtowerCategory {
    /** The site is a COSY frontend — the original use case these subdomains exist for. */
    COSY_FRONTEND,
    /** Some other legitimate use (blog, portfolio, docs, …). No reputation concern. */
    BENIGN,
    /** Shows patterns worth a human look, but nothing conclusive. */
    SUSPICIOUS,
    /** Clear abuse: scam, phishing, fake shop, malware distribution. */
    MALICIOUS,
    /** Nothing answered on the subdomain at scan time. */
    UNREACHABLE
}
