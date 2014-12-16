CREATE TABLE Realm (RealmID INT PRIMARY KEY NOT NULL,
                    RName VARCHAR(30) NOT NULL
                    UNIQUE (RName))
                    CHARACTER SET utf8 COLLATION utf8_bin;

CREATE TABLE PCharacter (CName VARCHAR(12) NOT NULL, -- Max name length is 12
                        Race INT NOT NULL, -- changed to race b/c that is what API gives us and we can derive faction from race but not visa-versa
                        RealmID INT NOT NULL,
                        PRIMARY KEY(CName, RealmID),
                        FOREIGN KEY(RealmID) REFERENCES Realm(RealmID))
                        CHARACTER SET utf8 COLLATION utf8_bin;

CREATE TABLE Item (ItemID INT NOT NULL,
                    -- will eventually turn into an INT once the String -> Int mapping is determined
                   Context VARCHAR(15) NOT NULL,
                   MaxStack INT NOT NULL,
                   IName VARCHAR(120) NOT NULL,
                   PRIMARY KEY(ItemID, Context))
                   CHARACTER SET utf8 COLLATION utf8_bin;

CREATE TABLE Listing (ListID BIGINT PRIMARY KEY NOT NULL,
                      Quantity INT NOT NULL,
                      BuyPrice BIGINT, -- allow NULL to indicate no buyout
                      BuyPricePerItem DOUBLE NOT NULL DEFAULT 0,
                      OriginalBidPrice BIGINT NOT NULL,
                      BidPrice BIGINT NOT NULL,
                      StartLength INT NOT NULL,
                      TimeLeft INT NOT NULL,
                      PostDate DATETIME NOT NULL,
                      CName VARCHAR(12) NOT NULL,
                      RealmID INT NOT NULL,
                      ItemID INT NOT NULL,
                       -- not used at this point, eventually will reference
                       -- Item.Context. Relationship is unclear at this point.
                      AContext INT NOT NULL,
                      Active SMALLINT NOT NULL,
                      FOREIGN KEY(CName) REFERENCES PCharacter(CName),
                      FOREIGN KEY(RealmID) REFERENCES Realm(RealmID),
                      FOREIGN KEY(ItemID) REFERENCES Item(ItemID))
                      CHARACTER SET utf8 COLLATION utf8_bin;

CREATE INDEX listing_realm_active ON Listing(RealmID, Active);
CREATE INDEX listing_bppi ON Listing(ItemID, BuyPricePerItem);

delimiter |

CREATE TRIGGER bppi_insert
BEFORE INSERT ON Listing
FOR EACH ROW
BEGIN
    SET NEW.BuyPricePerItem = NEW.BuyPrice / NEW.Quantity;
END;|

delimiter ;
