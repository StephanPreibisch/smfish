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

# Preprocessing
# 1. Polar transform 
# 2. Bin by x 
# 3. Transform from voxel space to distance space 
data <- cbind(data, ldply(apply(data, MARGIN=1, function(x) { polar_transform( x["y"], x["z"])} )))
voxel_dim <- data.frame(x=0.15, y=0.15, z=0.15)
for(dim in names(voxel_dim)) {
  print(voxel_dim[[dim]])
}

data$bin_x <- cut(data$x, breaks=9)

# Pairwise feature plot to get a feel for the data 
pdf(file="data/pairs_plot.pdf",width=7, height=7, useDingbats=F)
ggpairs(na.omit(data), columns=seq(1,6), params=c(alpha=1/2,size=0.5), color="id")
dev.off()

quartz()
p <- ggplot(data, aes(x=r, y=theta, color=id) )
p <- p + geom_point()
show(p)

quartz()
p <- ggplot(data, aes(x=y, y=z, color=id) )
p <- p + geom_point(size=1.5, alpha=1)
#p <- p + geom_density2d(data=cells)
#p <- p + geom_point(size=3, alpha=0.2, data=cells)
#p  <- p + stat_density2d()
p <- p + facet_wrap(~ bin_x, ncol=3)
p <- p + theme(legend.position = "bottom")
show(p)

