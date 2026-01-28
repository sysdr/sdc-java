grammar LogQuery;

query: selectClause whereClause? groupByClause? orderByClause? limitClause? EOF;

selectClause: SELECT fields;
fields: STAR | fieldList;
fieldList: IDENTIFIER (',' IDENTIFIER)*;

whereClause: WHERE condition;
condition:
    IDENTIFIER op value                         # SimpleCondition
    | condition AND condition                    # AndCondition
    | condition OR condition                     # OrCondition
    | '(' condition ')'                          # ParenCondition
    | function '(' IDENTIFIER ')' op value       # FunctionCondition
    ;

groupByClause: GROUP BY fieldList;
orderByClause: ORDER BY fieldList (ASC | DESC)?;
limitClause: LIMIT NUMBER;

function: COUNT | SUM | AVG | MIN | MAX;
op: EQ | NEQ | GT | LT | GTE | LTE | LIKE;
value: STRING | NUMBER | BOOLEAN;

// Lexer rules
SELECT: 'SELECT';
WHERE: 'WHERE';
GROUP: 'GROUP';
BY: 'BY';
ORDER: 'ORDER';
LIMIT: 'LIMIT';
AND: 'AND';
OR: 'OR';
ASC: 'ASC';
DESC: 'DESC';

COUNT: 'COUNT';
SUM: 'SUM';
AVG: 'AVG';
MIN: 'MIN';
MAX: 'MAX';

EQ: '=';
NEQ: '!=';
GT: '>';
LT: '<';
GTE: '>=';
LTE: '<=';
LIKE: 'LIKE';

STAR: '*';
BOOLEAN: 'true' | 'false';
NUMBER: [0-9]+ ('.' [0-9]+)?;
STRING: '\'' (~['])* '\'';
IDENTIFIER: [a-zA-Z_][a-zA-Z0-9_]*;

WS: [ \t\r\n]+ -> skip;
