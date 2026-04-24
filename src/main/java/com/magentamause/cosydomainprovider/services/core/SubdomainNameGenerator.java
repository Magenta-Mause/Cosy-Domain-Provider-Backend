package com.magentamause.cosydomainprovider.services.core;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class SubdomainNameGenerator {

    private static final List<String> ADJECTIVES =
            List.of(
                    "swift", "golden", "silver", "crystal", "amber", "coral", "azure", "misty",
                    "gentle", "bright", "calm", "crisp", "lush", "quiet", "silky", "warm", "brave",
                    "clear", "bold", "keen", "merry", "noble", "proud", "sleek", "smart", "vivid",
                    "cozy", "deep", "fair", "fresh", "glad", "grand", "happy", "light", "mild",
                    "neat", "pale", "pure", "rare", "rich", "rosy", "safe", "shiny", "slim", "soft",
                    "still", "wild", "wise", "fleet", "free", "full", "kind", "lone", "open",
                    "plain", "prime", "quick", "sharp", "stern", "stout", "tall", "tame", "true",
                    "vast", "lean", "cool", "hazy", "nimble", "breezy", "cloudy", "dewy", "earthy",
                    "frosty", "grassy", "leafy", "lunar", "mossy", "rainy", "sandy", "snowy",
                    "cosy");

    private static final List<String> NOUNS =
            List.of(
                    "harbor", "meadow", "forest", "valley", "river", "brook", "creek", "lake",
                    "pond", "ridge", "cliff", "cave", "cove", "bay", "reef", "dune", "marsh",
                    "delta", "grove", "glade", "heath", "moor", "fen", "glen", "peak", "summit",
                    "crest", "slope", "field", "patch", "bloom", "petal", "berry", "acorn", "cedar",
                    "birch", "maple", "willow", "oak", "pine", "elm", "ash", "ivy", "clover",
                    "thistle", "hawk", "wolf", "fox", "deer", "elk", "fawn", "hare", "swan",
                    "crane", "dove", "lark", "wren", "robin", "finch", "eagle", "raven", "sparrow",
                    "stone", "rock", "pebble", "gem", "cloud", "mist", "rain", "snow", "frost",
                    "breeze", "tide", "wave", "shore", "coast", "cape", "isle", "sand", "moss");

    public int poolSize() {
        return ADJECTIVES.size() * NOUNS.size();
    }

    public String generate() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        return ADJECTIVES.get(rng.nextInt(ADJECTIVES.size()))
                + "-"
                + NOUNS.get(rng.nextInt(NOUNS.size()));
    }
}
