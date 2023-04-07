/** https://github.com/gedoor/legado/tree/master/app/src/main/java/io/legado/app/data/entities */
interface BaseSource {
    lastUpdateTime?: number | undefined
}
interface BookSoure extends BaseSource {
    bookSourceUrl?: string | undefined
    bookSourceName?: string | undefined
    bookSourceType?: number | undefined
    bookSourceGroup?: string | undefined
    bookSourceComment?: string | undefined
    ruleSearch?: RuleSearch | undefined
    ruleBookInfo?: RuleBookInfo | undefined
    ruleToc?: RuleToc | undefined
    ruleContent?: RuleContent | undefined
    ruleReview?: RuleReview | undefined
    ruleExplore?: ruleExplore | undefined
}
interface RuleSearch {
    checkKeyWord?: string | undefined
}
interface RssSource extends BaseSource {
    sourceUrl?: string | undefined
    sourceName?: string | undefined
    sourceGroup?: string | undefined
    sourceComment?: string | undefined
}
type Source = BookSoure & RssSource

export { Source, BookSoure, RssSource }
