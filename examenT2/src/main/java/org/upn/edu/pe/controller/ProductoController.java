
package org.upn.edu.pe.controller;
import java.util.Date;

import java.util.ArrayList;
import java.util.List;

import org.upn.edu.pe.model.Detalle;
import org.upn.edu.pe.model.Venta;
import org.upn.edu.pe.model.Producto;
import org.upn.edu.pe.repository.IDetalleRepository;
import org.upn.edu.pe.repository.IProductoRepository;
import org.upn.edu.pe.repository.IVentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.SessionAttributes;

@Controller
@SessionAttributes({"carrito","total","env","des","subtotal"})
public class ProductoController {
	
	// Inicializacion del objeto carrito
	@ModelAttribute("carrito")
	public List<Detalle> getCarrito(){
		return new ArrayList<>();
	}
	
	// Inicializacion del objeto total
	@ModelAttribute("total")
	public double getTotal() {
		return 0.0;
	}
	
	// Declaracion e Inicializacion de objetos para el control del carrito de compras
	@Autowired
	private IProductoRepository productoRepository;
	
	@Autowired
	private IVentaRepository ventaRepository;
	
	@Autowired
	private IDetalleRepository detalleRepository;
	
	// Método para visualizar los productos a vender
	@GetMapping("/index")						// localhost:9090/index
	public String listado(Model model) {
		List<Producto> lista = new ArrayList<>();
		lista = productoRepository.findAll();	// Recuperar las filas de la tabla productos
		model.addAttribute("productos", lista);
		return "index";
	}
	
	//eliminar productos 
	@GetMapping("/eliminar/{idProducto}")
	public String eliminarProducto(Model model, @PathVariable(name = "idProducto") int idProducto) {
		 System.out.println("Método eliminarProducto llamado con idProducto: " + idProducto);

		List<Detalle> carrito = (List<Detalle>) model.getAttribute("carrito");
	    
	    Detalle detalleAEliminar = null;
	    for (Detalle d : carrito) {
	        if (d.getProducto().getIdProducto() == idProducto) {
	            detalleAEliminar = d;
	            break;
	        }
	    }
	    if (detalleAEliminar != null) {
	        carrito.remove(detalleAEliminar);
	    }
	    
	    calcular(carrito,model);
	    

	    return "redirect:/carrito";
	}

	
	
	// Método para agregar productos al carrito
	@GetMapping("/agregar/{idProducto}")
	public String agregar(Model model,@PathVariable(name="idProducto",required = true) int idProducto) {
		Producto p = productoRepository.findById(idProducto).orElse(null);
		List<Detalle> carrito = (List<Detalle>)model.getAttribute("carrito");
		double total = 0.0;
		double cos_envio=0.0;
		double mon_descuento=0.0;
		double subtotal=0.0;
		boolean existe = false;
		Detalle detalle = new Detalle();
		if(p != null) {
			detalle.setProducto(p);
			detalle.setCantidad(1);
			detalle.setSubtotal(detalle.getProducto().getPrecio() * detalle.getCantidad());
		}
		// Si el carrito esta vacio
		if(carrito.size() == 0) {
			carrito.add(detalle);
		}else {
				for(Detalle d : carrito) {
					if(d.getProducto().getIdProducto() == p.getIdProducto()) {
						d.setCantidad(d.getCantidad() + 1);
						d.setSubtotal(d.getProducto().getPrecio() * d.getCantidad());
						existe = true;
					}
				}
				if(!existe)carrito.add(detalle);
		}
		
		calcular(carrito,model);
		model.addAttribute("carrito", carrito);
		return "redirect:/index";
	}
	
	//calcular 
	private void calcular(List<Detalle> carrito, Model model) {
		 double total = 0.0;
		    double cos_envio = 0.0;
		    double mon_descuento = 0.0;
		    double subtotal = 0.0;

		    for (Detalle d : carrito) subtotal += d.getSubtotal();

		    cos_envio = subtotal * 0.10; // 10 porciento
		    mon_descuento = subtotal * -0.25; // 25 porciento
		    total = subtotal + cos_envio + mon_descuento;

		    // Guarda los valores actualizados en la sesión
		    model.addAttribute("total", total);
		    model.addAttribute("env", cos_envio);
		    model.addAttribute("des", mon_descuento);
		    model.addAttribute("subtotal", subtotal);
		
	}
	
	
	@GetMapping("/pagar")
	public String guardarCarrito(Model model) {
	    List<Detalle> carrito = (List<Detalle>) model.getAttribute("carrito");
	    
	    Venta n_Venta = new Venta();
	    n_Venta.setFechaRegistro(new Date());
	    double montoTotal = (double) model.getAttribute("total");
	    n_Venta.setMontoTotal(montoTotal);

	     ventaRepository.save(n_Venta);

	    int idVentaGenerado = n_Venta.getIdVenta();

	    for (Detalle detalle : carrito) {
	        Detalle n_Detalle = new Detalle();

	        n_Detalle.setVenta(n_Venta);
	        n_Detalle.setProducto(detalle.getProducto());
	        n_Detalle.setCantidad(detalle.getCantidad());
	        n_Detalle.setSubtotal(detalle.getSubtotal());

	        detalleRepository.save(n_Detalle);

	    }
	    carrito.clear();

	    return "redirect:/index";
	}

	
	
	
	// Método para visualizar el carrito de compras
	@GetMapping("/carrito")
	public String carrito() {
		return "carrito";
	}
}
