
CREATE TABLE users(
	userID nvarchar(18) NOT NULL,
	userName nvarchar(100) NOT NULL,
	userToken nvarchar(200) NOT NULL,
	userIP nvarchar(50) NOT NULL,
	userPort int NOT NULL,
	userPic nvarchar(200) NULL,
 CONSTRAINT [PK_users] PRIMARY KEY CLUSTERED 
(
	[userID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]


