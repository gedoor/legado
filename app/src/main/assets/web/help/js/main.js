require.config({
    baseUrl: 'js',
    paths: {
        marked: 'marked.min',
        markedHighlight: 'marked-highlight.umd',
        highlight: 'highlight.min',
    },
    shim: {
        marked: {
            exports: 'marked',
        },
        markedHighlight: {
            exports: 'markedHighlight',
        },
        highlight: {
            exports: 'hljs',
        },
    },
});

require(['marked', 'markedHighlight', 'highlight'], (marked, mdhl, hljs) => {
    marked.use(
        mdhl.markedHighlight({
            langPrefix: 'theme-vs2015-min hljs language-',
            highlight(code, lang) {
                const language = hljs.getLanguage(lang) ? lang : 'txt';
                const result = hljs.highlight(code, {language});
                return result.value;
            },
        })
    );

    const path = '/help/md/';
    const file = location.hash.slice(1).trim();
    if (!file) return;
    fetch(`${path}${file}.md`)
        .then((response) => response.text())
        .then((md_text) => {
            document.getElementById('mdviewer').innerHTML = marked.parse(md_text);
        });
});
