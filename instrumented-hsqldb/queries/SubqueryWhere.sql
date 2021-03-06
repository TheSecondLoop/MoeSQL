CREATE TABLE products ( "Product" VARCHAR(50), "Type" int, "Price" FLOAT )

INSERT INTO products VALUES ('A',1,5), ('A',2,3), ('A',3,3), ('B',1,10), ('B',2,8), ('C', 1, 7)

SELECT t1."Product", t1."Type", t1."Price"
FROM products AS t1
WHERE t1."Price" = (SELECT MAX(t2."Price") FROM products AS t2 WHERE t1."Product" = t2."Product")

DROP TABLE products