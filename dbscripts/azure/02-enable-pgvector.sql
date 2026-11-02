-- For the future Spring AI / pgvector work. First allow-list the extension (Cloud Shell):
--   az postgres flexible-server parameter set --resource-group <rg> --server-name <server> --name azure.extensions --value vector
-- Then run this as the server admin, connected to the "tickets" database:
create extension if not exists vector;
