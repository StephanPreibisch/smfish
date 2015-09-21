###############################
# Plot splicing maps
# 1. Load data
# 2. Polar transform
###############################

# Load dependencies
library(rgl); library(plyr); library(GGally); library(ggplot2)
sapply(list.files(pattern="[.]R$", path="R", full.names=TRUE), source)

# Load data
files  <- list(cells="data/cells.straight.txt", guide="data/guideRNA.straight.txt", alt="data/altexonRNA.straight.txt")
data <- ldply(files, .fun=function(x) {read.table(x, header=F, sep="\t", col.names=c("id","x","y","z"))} )
data$id <- as.factor(data$.id)
data$.id <- NULL

# Polar transform 
data <- cbind(data, ldply(apply(data, MARGIN=1, function(x) { polar_transform( x["y"], x["z"])} )))

# Pairwise feature plot to get a feel for the data 
pdf(file="data/pairs_plot.pdf",width=7, height=7, useDingbats=F)
ggpairs(na.omit(data), columns=seq(1,6), params=c(alpha=1/2,size=0.5), color="id")
dev.off()
