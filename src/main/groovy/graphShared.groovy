def fixedEngineColors = [
        ChronicleMap       : '#1F77B4',
        H2                 : '#2CA02C',
        HestiaStoreBasic   : '#E53935',
        HestiaStoreCompress: '#E53935',
        HestiaStoreStream  : '#E53935',
        LevelDB            : '#ebeb49',
        MapDB              : '#9467BD',
        RocksDB            : '#17BECF',
        Unknown            : '#9C755F'
].asImmutable()

def canonicalEngineName = { String engine ->
    String value = engine?.trim()
    if (!value) {
        return 'Unknown'
    }
    if (value.startsWith('HestiaStoreBasic')) {
        return 'HestiaStoreBasic'
    }
    if (value.startsWith('HestiaStoreCompress')) {
        return 'HestiaStoreCompress'
    }
    if (value.startsWith('HestiaStoreStream')) {
        return 'HestiaStoreStream'
    }
    return value
}

def colorForEngine = { String engine ->
    String canonical = canonicalEngineName(engine)
    String color = fixedEngineColors[canonical]
    if (color == null) {
        throw new IllegalArgumentException(
                "No color mapping configured for engine '${engine}' " +
                "(canonical: '${canonical}').")
    }
    color
}

def darker = { String hex ->
    int rgb = Integer.parseInt(hex.substring(1), 16)
    int r = ((rgb >> 16) & 0xFF)
    int g = ((rgb >> 8) & 0xFF)
    int b = (rgb & 0xFF)
    r = Math.max(0, (int) (r * 0.85))
    g = Math.max(0, (int) (g * 0.85))
    b = Math.max(0, (int) (b * 0.85))
    String.format('#%02X%02X%02X', r, g, b)
}

return [
        fixedEngineColors : fixedEngineColors,
        canonicalEngineName: canonicalEngineName,
        colorForEngine    : colorForEngine,
        darker            : darker
]
