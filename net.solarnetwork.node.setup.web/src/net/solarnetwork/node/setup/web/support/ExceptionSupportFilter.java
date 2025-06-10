/* ==================================================================
 * ExceptionSupportFilter.java - 3/03/2022 9:57:06 AM
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.node.setup.web.support;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import org.apache.tiles.request.render.CannotRenderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.GenericFilterBean;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import net.solarnetwork.node.Constants;

/**
 * Filter to catch errors like view rendering exceptions.
 * 
 * @author matt
 * @version 1.0
 * @since 2.3
 */
public class ExceptionSupportFilter extends GenericFilterBean implements Filter {

	private static final Logger log = LoggerFactory.getLogger(ExceptionSupportFilter.class);

	/**
	 * Default constructor.
	 */
	public ExceptionSupportFilter() {
		super();
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		try {
			chain.doFilter(request, response);
		} catch ( Throwable t ) {
			if ( !(response instanceof HttpServletResponse) ) {
				throw t;
			}
			final HttpServletResponse res = (HttpServletResponse) response;
			Throwable e = t;
			while ( e != null ) {
				if ( e instanceof CannotRenderException ) {
					handleCannotRenderException((CannotRenderException) e);
					res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
							"Corrupted JSP; please try again.");
					return;
				}
				e = e.getCause();
			}
			throw t;
		}
	}

	private void handleCannotRenderException(CannotRenderException e) {
		// try to delete the work dir to remove corrupted JSP class
		String homeDir = Constants.solarNodeHome();
		if ( homeDir == null ) {
			return;
		}
		Path workPath = Paths.get(homeDir, "work");
		if ( Files.notExists(workPath) ) {
			workPath = Paths.get(homeDir, "var", "work");
			if ( Files.notExists(workPath) ) {
				return;
			}
		}
		log.warn("JSP rendering exception; will delete generated classes from {}: {}", workPath, e);
		try {
			Files.walkFileTree(workPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
					new SimpleFileVisitor<Path>() {

						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
								throws IOException {
							try {
								String name = file.getFileName().toString().toLowerCase();
								if ( name.endsWith(".java") || name.endsWith(".class") ) {
									Files.delete(file);
								}
							} catch ( IOException e ) {
								log.warn("Error deleting file [{}] while cleaning work dir: {}", file,
										e);
							}
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult visitFileFailed(Path file, IOException e)
								throws IOException {
							log.warn("Failed to visit file [{}] while cleaning work dir: {}", file, e);
							return FileVisitResult.CONTINUE;
						}

					});
		} catch ( IOException e2 ) {
			log.warn("IOException cleaning out work dir!", e2);
		}
	}

}
