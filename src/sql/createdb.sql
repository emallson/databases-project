CREATE TABLE Realm (RealmID INT PRIMARY KEY NOT NULL,
                    RName VARCHAR(30) NOT NULL);

CREATE TABLE PCharacter (CName VARCHAR(12) PRIMARY KEY NOT NULL, -- Max name length is 12
                        Race INT NOT NULL, -- changed to race b/c that is what API gives us and we can derive faction from race but not visa-versa
                        RealmID INT NOT NULL,
                        FOREIGN KEY(RealmID) REFERENCES Realm(RealmID));

CREATE TABLE Item (ItemID INT PRIMARY KEY NOT NULL,
                   MaxStack INT NOT NULL,
                   IName VARCHAR(120) NOT NULL);

CREATE TABLE Listing (ListID INT PRIMARY KEY NOT NULL,
                      Quantity INT NOT NULL,
                      BuyPrice INT, -- allow NULL to indicate no buyout
                      BidPrice INT NOT NULL,
                      StartLength INT NOT NULL,
                      TimeLeft INT NOT NULL,
                      PostDate TIMESTAMP NOT NULL,
                      CName VARCHAR(12) NOT NULL,
                      RealmID INT NOT NULL,
                      ItemID INT NOT NULL,
                      FOREIGN KEY(CName) REFERENCES PCharacter(CName),
                      FOREIGN KEY(RealmID) REFERENCES Realm(RealmID),
                      FOREIGN KEY(ItemID) REFERENCES Item(ItemID));
