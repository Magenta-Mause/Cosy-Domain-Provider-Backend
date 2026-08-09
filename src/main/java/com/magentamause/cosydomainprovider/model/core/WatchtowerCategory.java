package com.magentamause.cosydomainprovider.model.core;

/** What the nightly Watchtower agent concluded a subdomain is being used for. */
public enum WatchtowerCategory {
    /** The site is a COSY frontend — the original use case these subdomains exist for. */
    COSY_FRONTEND,
    /** Some other legitimate use (blog, portfolio, docs, …). No reputation concern. */
    BENIGN,
    /**
     * The host answered but there is nothing on it — a parked or not-yet-configured subdomain,
     * typically 404 on every path. Distinct from {@link #UNREACHABLE}, which means nothing answered
     * at all, and from {@link #BENIGN}, which implies actual content someone put there. Without
     * this the agent had to force an empty site into one of those two and picked differently
     * between runs.
     */
    EMPTY,
    /** Shows patterns worth a human look, but nothing conclusive. */
    SUSPICIOUS,
    /** Clear abuse: scam, phishing, fake shop, malware distribution. */
    MALICIOUS,
    /** Nothing answered on the subdomain at scan time. */
    UNREACHABLE
}
