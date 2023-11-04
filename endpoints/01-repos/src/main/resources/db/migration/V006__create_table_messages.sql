CREATE TABLE IF NOT EXISTS messages(
  id UUID NOT NULL PRIMARY KEY,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  "to" VARCHAR NOT NULL,
  text VARCHAR NOT NULL,
  status DELIVERY_STATUS NOT NULL
);