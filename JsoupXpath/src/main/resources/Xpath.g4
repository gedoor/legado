grammar Xpath;

/*
XPath 1.0 grammar. Should conform to the official spec at
http://www.w3.org/TR/1999/REC-xpath-19991116. The grammar
rules have been kept as close as possible to those in the
spec, but some adjustmewnts were unavoidable. These were
mainly removing left recursion (spec seems to be based on
LR), and to deal with the double nature of the '*' token
(node wildcard and multiplication operator). See also
section 3.7 in the spec. These rule changes should make
no difference to the strings accepted by the grammar.

Written by Jan-Willem van den Broek
Version 1.0

Do with this code as you will.
*/
/*
    Ported to Antlr4 by Tom Everett <tom@khubla.com>
*/
/**
操作符扩展：
    a^=b 字符串a以字符串b开头 a startwith b
    a*=b a包含b, a contains b
    a$=b a以b结尾 a endwith b
    a~=b a的内容符合 正则表达式b
    a!~b a的内容不符合 正则表达式b

轴扩展：
    following-sibling-one
    preceding-sibling-one
    sibling

NodeTest扩展：
    num            抽取数字
    allText        提取节点下全部文本
    outerHtml      获取全部节点的 包含节点本身在内的全部html
    html           获取全部节点的内部的html

*/

main  :  expr
  ;

locationPath
  :  relativeLocationPath
  |  absoluteLocationPathNoroot
  ;

absoluteLocationPathNoroot
  :  op=(PATHSEP|ABRPATH) relativeLocationPath
  ;

relativeLocationPath
  :  step (op=(PATHSEP|ABRPATH) step)*
  ;

step
  :  axisSpecifier nodeTest predicate*
  |  abbreviatedStep
  ;

axisSpecifier
  :  AxisName '::'
  |  '@'?
  ;

nodeTest:  nameTest
  |  NodeType '(' ')'
  |  'processing-instruction' '(' Literal ')'
  ;

predicate
  :  '[' expr ']'
  ;

abbreviatedStep
  :  '.'
  |  '..'
  ;

expr  :  orExpr
  ;

primaryExpr
  :  variableReference
  |  '(' expr ')'
  |  Literal
  |  Number
  |  functionCall
  ;

functionCall
  :  functionName '(' ( expr ( ',' expr )* )? ')'
  ;

unionExprNoRoot
  :  pathExprNoRoot (op=PIPE unionExprNoRoot)?
  |  PATHSEP PIPE unionExprNoRoot
  ;

pathExprNoRoot
  :  locationPath
  |  filterExpr (op=(PATHSEP|ABRPATH) relativeLocationPath)?
  ;

filterExpr
  :  primaryExpr predicate*
  ;

orExpr  :  andExpr ('or' andExpr)*
  ;

andExpr  :  equalityExpr ('and' equalityExpr)*
  ;

equalityExpr
  :  relationalExpr (op=(EQUALITY|INEQUALITY) relationalExpr)*
  ;

relationalExpr
  :  additiveExpr (op=(LESS|MORE_|LESS|GE|START_WITH|END_WITH|CONTAIN_WITH|REGEXP_WITH|REGEXP_NOT_WITH) additiveExpr)*
  ;

additiveExpr
  :  multiplicativeExpr (op=(PLUS|MINUS) multiplicativeExpr)*
  ;

multiplicativeExpr
  :  unaryExprNoRoot (op=(MUL|DIVISION|MODULO) multiplicativeExpr)?
//  |  '/' (op=('`div`'|'`mod`') multiplicativeExpr)?
  ;

unaryExprNoRoot
  :  (sign=MINUS)? unionExprNoRoot
  ;

qName  :  nCName (':' nCName)?
  ;

functionName
  :  qName  // Does not match nodeType, as per spec.
  ;

variableReference
  :  '$' qName
  ;

nameTest:  '*'
  |  nCName ':' '*'
  |  qName
  ;

nCName  :  NCName
  |  AxisName
  ;

NodeType:  'comment'
  |  'text'
  |  'processing-instruction'
  |  'node'
  |  'num'                                              //抽取数字
  |  'allText'                                          //提取节点下全部文本
  |  'outerHtml'                                        //获取全部节点的 包含节点本身在内的全部html
  |  'html'                                             //获取全部节点的内部的html
  ;

Number  :  Digits ('.' Digits?)?
  |  '.' Digits
  ;

fragment
Digits  :  ('0'..'9')+
  ;

AxisName:  'ancestor'
  |  'ancestor-or-self'
  |  'attribute'
  |  'child'
  |  'descendant'
  |  'descendant-or-self'
  |  'following'
  |  'following-sibling'
//  |  'namespace'
  |  'parent'
  |  'preceding'
  |  'preceding-sibling'
  |  'self'
  |  'following-sibling-one'
  |  'preceding-sibling-one'
  |  'sibling'
  ;


  PATHSEP 
       :'/';
  ABRPATH
       : '//';
  LPAR   
       : '(';
  RPAR   
       : ')';
  LBRAC   
       :  '[';
  RBRAC   
       :  ']';
  MINUS   
       :  '-';
  PLUS   
       :  '+';
  DOT   
       :  '.';
  MUL   
       : '*';
  DIVISION
       : '`div`';
  MODULO
       : '`mod`';
  DOTDOT   
       :  '..';
  AT   
       : '@';
  COMMA  
       : ',';
  PIPE   
       :  '|';
  LESS   
       :  '<';
  MORE_ 
       :  '>';
  LE   
       :  '<=';
  GE   
       :  '>=';
  EQUALITY
       :  '=';
  INEQUALITY
       :  '!=';
  START_WITH
       :  '^=';
  END_WITH
       :  '$=';
  CONTAIN_WITH
       :  '*=';
  REGEXP_WITH
       :  '~=';
  REGEXP_NOT_WITH
       :  '!~';
  COLON
       :  ':';
  CC   
       :  '::';
  APOS   
       :  '\'';
  QUOT   
       :  '"';
  
Literal  :  '"' ~'"'* '"'
  |  '\'' ~'\''* '\''
  ;

Whitespace
  :  (' '|'\t'|'\n'|'\r')+ ->skip
  ;

NCName  :  NCNameStartChar NCNameChar*
  ;

fragment
NCNameStartChar
  :  'A'..'Z'
  |   '_'
  |  'a'..'z'
  |  '\u00C0'..'\u00D6'
  |  '\u00D8'..'\u00F6'
  |  '\u00F8'..'\u02FF'
  |  '\u0370'..'\u037D'
  |  '\u037F'..'\u1FFF'
  |  '\u200C'..'\u200D'
  |  '\u2070'..'\u218F'
  |  '\u2C00'..'\u2FEF'
  |  '\u3001'..'\uD7FF'
  |  '\uF900'..'\uFDCF'
  |  '\uFDF0'..'\uFFFD'
// Unfortunately, java escapes can't handle this conveniently,
// as they're limited to 4 hex digits. TODO.
//  |  '\U010000'..'\U0EFFFF'
  ;

fragment
NCNameChar
  :  NCNameStartChar | '-' | '.' | '0'..'9'
  |  '\u00B7' | '\u0300'..'\u036F'
  |  '\u203F'..'\u2040'
  ;