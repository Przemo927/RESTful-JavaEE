package pl.przemek.rest;

import org.json.simple.JSONObject;
import pl.przemek.mapper.ExceptionMapperAnnotation;
import pl.przemek.model.Discovery;
import pl.przemek.model.User;
import pl.przemek.rest.utils.ResponseUtils;
import pl.przemek.service.DiscoveryService;
import pl.przemek.wrapper.ResponseMessageWrapper;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/discovery")
@ExceptionMapperAnnotation
public class DiscoveryEndPoint {

	private DiscoveryService discoveryService;
	private HttpServletRequest request;
	private Logger logger;
	@Context
	UriInfo uriInfo;
	@Context
	Request requestRest;

	@Inject
	public DiscoveryEndPoint(Logger logger,DiscoveryService discoveryService, HttpServletRequest request){
		this.logger=logger;
		this.discoveryService=discoveryService;
		this.request=request;
	}
	public DiscoveryEndPoint(){}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addDiscovery(@Valid Discovery discovery) {
		try {
			request.setCharacterEncoding("UTF-8");
			HttpSession session=request.getSession(false);
			User user = (User) session.getAttribute("user");
			if(user!=null) {
				discovery.setUser(user);
				discoveryService.addDiscovery(discovery);
			}else{
				logger.log(Level.SEVERE,"[DiscoveryEndPoint] addDiscovery() user wasn't saved in session");
				return Response.status(Response.Status.UNAUTHORIZED).build();
			}
		} catch (UnsupportedEncodingException e) {
			logger.log(Level.SEVERE,"[DiscoveryEndPoint] addDiscovery()",e);
			Response.status(Response.Status.BAD_REQUEST).build();
		}

		return Response.created(URI.create(uriInfo.getAbsolutePath()+ResponseUtils.URL_SEPARATOR+discovery.getId())).build();
	}

	@GET
	@Path("/{id : \\d+}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getById(@PathParam("id") long id) {
		Optional<Discovery> discoveryOptional=discoveryService.getById(id);
		return discoveryOptional.map(discovery -> {
			return ResponseUtils.checkIfModifiedAndReturnResponse(discovery,requestRest).build();
		}).orElseGet(()->{
			logger.log(Level.SEVERE,"[DiscoveryEndPoint] getById() discovery wasn't found");
			return Response.status(Response.Status.NO_CONTENT).build();
		});
	}

    @DELETE
    @Path("/{id : \\d+}")
    public Response removeById(@PathParam("id") long id) {
        discoveryService.removeDiscoveryById(id);
		return Response.ok("Discovery was removed").build();

    }

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getALL(@QueryParam("orderBy") String order,@QueryParam("beginWith") Integer begin,
						   @QueryParam("quantity") Integer quantity) {
		List<Discovery> allDiscoveries= Collections.emptyList();
		if(begin!=null && !(begin<0) && quantity!=null && !(quantity<0)){
			if("date".equals(order)){
				allDiscoveries=discoveryService.getWithLimitOrderByDate(begin,quantity);
			}else {
				allDiscoveries = discoveryService.getWithLimit(begin, quantity);
			}
		}
		if(allDiscoveries.isEmpty()){
			logger.log(Level.SEVERE,"[DiscoveryEndPoint] getALL() discovery wasn't found");
			return Response.status(Response.Status.NO_CONTENT).build();
		}
		for(Discovery discovery:allDiscoveries)
			discovery.setComments(null);
		return ResponseUtils.checkIfModifiedAndReturnResponse(allDiscoveries,requestRest).build();
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateDiscovery(Discovery discovery) {
		if(discovery!=null) {
			discoveryService.updateDiscovery(discovery);
			return Response.ok(ResponseMessageWrapper.wrappMessage("Discovery was updated")).build();
		}else {
			logger.log(Level.SEVERE,"[DiscoveryEndPoint] updateDiscovery() discovery is null");
			return Response.status(Response.Status.NOT_MODIFIED).build();
		}
	}
	@GET
	@Path("quantity")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getQuantityOfDiscoveries(){
		BigInteger quantityOfDiscoveries=discoveryService.getQuantityOfDiscoveries();
		JSONObject jsonObject=ResponseMessageWrapper.wrappMessage(String.valueOf(quantityOfDiscoveries));
		return ResponseUtils.checkIfModifiedAndReturnResponse(jsonObject,requestRest).build();
	}


}
