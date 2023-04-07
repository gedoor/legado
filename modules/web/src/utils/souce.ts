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

export const isSourceContains: (source: Source, searchKey: string) => boolean = (source, searchKey) => {
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

export const emptyBookSource = {
    ruleSearch: {},
    ruleBookInfo: {},
    ruleToc: {},
    ruleContent: {},
    ruleReview: {},
    ruleExplore: {}
}
export const emptyRssSource = {}
