library("COUNT")
library("survival")

data("lung")
data("rwm1984")

loadVisit = function(){
	visit = data.frame(rwm1984)
	visit$edlevel = factor(visit$edlevel, labels = c("Not HS grad", "HS grad", "Coll/Univ", "Grad School"))
	visit$edlevel1 = NULL
	visit$edlevel2 = NULL
	visit$edlevel3 = NULL
	visit$edlevel4 = NULL
	visit$hospvis = NULL

	# Move docvis from the first position to the last position
	visit = visit[, c(2:ncol(visit), 1)]

	return (visit)
}

visit = loadVisit()

# Drop rows with 0 counts
visit = visit[visit$docvis > 0, ]

write.table(visit, "csv/Visit.csv", sep = ",", quote = FALSE, row.names = FALSE)

loadLung = function(){
	lung = data.frame(lung)
	lung$inst = NULL
	lung$sex = as.factor(lung$sex)
	lung$status = as.factor(lung$status - 1)

	# Move time and status from the first position to the last position
	lung = lung[, c(3:ncol(lung), 1, 2)]

	return (lung)
}

lung = loadLung()

write.table(lung, "csv/LungNA.csv", sep = ",", quote = FALSE, row.names = FALSE)
