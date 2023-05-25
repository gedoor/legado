import { Source } from '../source'


const isNullOrBlank = (string: string | null | undefined | number) => string == null || (string as string).length === 0 || /^\s+$/.test(string as string)
const isBookSource = (source: Source) => "bookSourceName" in source

export const isInvaildSource: (source: Source) => boolean = (source) => {
    if (isBookSource(source)) {
        return !isNullOrBlank(source.bookSourceName) &&
            !isNullOrBlank(source.bookSourceUrl) &&
            !isNullOrBlank(source.bookSourceType)
    }
    return !isNullOrBlank(source.sourceName) &&
        !isNullOrBlank(source.sourceName)
}

export const getSourceUniqueKey = (source: Source) => isBookSource(source) ? source.bookSourceUrl : source.sourceUrl;

export const isSourceMatches: (source: Source, searchKey: string) => boolean = (source, searchKey) => {
    // TODO: 正则和普通字符串识别 识别 * . \ [ ] <= <! != = ?: () \d\w\s\...
    if (isBookSource(source)) {
        return (source.bookSourceName?.includes(searchKey) ||
            source.bookSourceUrl?.includes(searchKey) ||
            source.bookSourceGroup?.includes(searchKey) ||
            source.bookSourceComment?.includes(searchKey)) ?? false
    }
    return (source.sourceName?.includes(searchKey) ||
        source.sourceUrl?.includes(searchKey) ||
        source.sourceGroup?.includes(searchKey) ||
        source.sourceComment?.includes(searchKey)) ?? false
}

export const convertSourcesToMap = (sources: Source[]): Map<string, Source> => {
    const map = new Map();
    sources.forEach((source) =>
        map.set(getSourceUniqueKey(source), source)
    );
    return map;
}

export const emptyBookSource = {
    ruleSearch: {},
    ruleBookInfo: {},
    ruleToc: {},
    ruleContent: {},
    ruleReview: {},
    ruleExplore: {}
}
export const emptyRssSource = {}
