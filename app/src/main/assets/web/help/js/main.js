require.config({
    baseUrl: 'js',
    paths: {
        markdownit: 'markdown-it.min',
        highlight: 'highlight.min',
    },
    shim: {
        highlight: {
            exports: 'hljs',
        },
    },
});

require(['markdownit', 'highlight'], (mdit, hljs) => {
    const md = mdit({
        html: false, // 在源代码中启用 HTML 标签
        xhtmlOut: false, // 使用“/”关闭单个标签 (<br />)。 这仅用于完全兼容 CommonMark。
        breaks: false, // 将段落中的 '\n' 转换为 <br>
        langPrefix: 'language-', // 代码块的 CSS 属性前缀。用于代码高亮插件。
        linkify: false, // 自动将类似 URL 的文本转换为链接
        typographer: false, // 启用一些语言中立的替换+引号美化
        quotes: '“”‘’',
        highlight: function (code, lang) {
            let language = hljs.getLanguage(lang)?.name ?? 'plaintext';
            if (language.includes('HTML')) language = 'xml';
            return hljs.highlight(code, {language}).value;
        },
    });

    const path = '../md/';
    const file = location.hash.slice(1).trim();
    if (!file) return;
    fetch(`${path}${file}.md`)
        .then((response) => response.text())
        .then((md_text) => {
            document.getElementById('mdviewer').innerHTML = md.render(md_text);
        });
});
