#' Transform xy coordinates into polar form
#'
#' @description
#' \code{polar_transform} Transform xy coordinates into polar form
#'
#' @details
#' This function takes a point with xy coordinates and transforms it into 
#' its distance and angle from the origin. 
#' 
#' @param P0 A numerical vector of length 2 holding xy values. 
#' @return A numerical vector of length 2 theta and r values. 
#' 
#' @family transformation functions
polar_transform <- function(x,y) {
  x = as.numeric(x)
  y = as.numeric(y)
  
  # Get distance from origin
  r = sqrt(x^2+y^2)
  
  # Determine angle. 
  theta = atan(abs(y/x))
  # Adjust theta according to the quadrant. 
  
  if( x < 0 & y > 0 ) { # QII
    theta = pi - theta
  } else if( x < 0 & y < 0 ) { # QIII
    theta = pi + theta
  } else if( x > 0 & y < 0) { # QIV
    theta = 2*pi - theta
  } 
  return( data.frame(theta=theta, r=r) )
}