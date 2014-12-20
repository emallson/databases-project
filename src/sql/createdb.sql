CREATE TABLE Realm (RealmID INT PRIMARY KEY NOT NULL,
                    RName VARCHAR(30) NOT NULL UNIQUE);

CREATE TABLE PCharacter (CName VARCHAR(12) NOT NULL, -- Max name length is 12
                        Race INT NOT NULL, -- changed to race b/c that is what API gives us and we can derive faction from race but not visa-versa
                        RealmID INT NOT NULL,
                        PRIMARY KEY(CName, RealmID)
                        FOREIGN KEY(RealmID) REFERENCES Realm(RealmID));

CREATE TABLE Item (ItemID INT NOT NULL,
                    -- will eventually turn into an INT once the String -> Int mapping is determined
                   Context VARCHAR(15) NOT NULL,
                   MaxStack INT NOT NULL,
                   IName VARCHAR(120) NOT NULL,
                   PRIMARY KEY(ItemID, Context));

CREATE TABLE Listing (ListID BIGINT PRIMARY KEY NOT NULL,
                      Quantity INT NOT NULL,
                      BuyPricePerItem REAL, -- allow NULL to indicate no buyout price.
                      OriginalBidPrice BIGINT NOT NULL,
                      BidPrice BIGINT NOT NULL,
                      StartLength INT NOT NULL,
                      TimeLeft INT NOT NULL,
                      PostDate TIMESTAMP NOT NULL,
                      CName VARCHAR(12) NOT NULL,
                      RealmID INT NOT NULL,
                      ItemID INT NOT NULL,
                       -- AContext is not used at this point, eventually will reference
                       -- Item.Context. Relationship is unclear at this
                       -- point. Blizzard has yet to publish documentation for
                       -- this field.
                      Context VARCHAR(15) NOT NULL,
                      AContext INT NOT NULL,
                      Active SMALLINT NOT NULL,
                      FOREIGN KEY(CName, RealmID) REFERENCES PCharacter(CName, RealmID),
                      FOREIGN KEY(ItemID, Context) REFERENCES Item(ItemID, Context));

CREATE INDEX listing_realm_active ON Listing(RealmID, Active);
CREATE INDEX listing_bppi ON Listing(ItemID, BuyPricePerItem);
