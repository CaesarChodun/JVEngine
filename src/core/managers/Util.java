package core.managers;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.Buffer;

import core.employees.PhysicalDevice;
import core.employees.ColorFormatAndSpace;
import core.employees.LogicalDevice;

import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.PointerBuffer;

/**
 * @author cezarychodun
 *
 */
public class Util {
	
	/**
	 * 
	 * @param texts
	 * @return
	 */
	public static ByteBuffer[] makeByteBuffers(String[] texts) {
		ByteBuffer[] out = new ByteBuffer[texts.length];
		
		for(int i = 0; i < texts.length; i++)
			out[i] = memUTF8(texts[i]);
		
		return out;
	}
	
	/**
	 * <h5>Description:</h5>
	 * <p>Creates a pointer buffer from given buffers.</p>
	 * @param buffers	- Buffers to be converted into pointer buffer.
	 * @return			- Pointer buffer that points to given buffers.
	 */
	public static PointerBuffer makePointer(ByteBuffer[] buffers) {
		PointerBuffer pb = memAllocPointer(buffers.length);
		
		for(ByteBuffer b : buffers)
			pb.put(b);
		pb.flip();
		
		return pb;
	}
	
	/**
	 * <h5>Description:</h5>
	 * <p>Frees contents of buffers.</p>
	 * @param buffers	- Buffers to free.
	 */
	public static void freeBuffers(Buffer...buffers) {
		for(Buffer bb : buffers)
			memFree(bb);
	}

	/**
	 * <h5>Description:</h5>
	 * <p>
	 * 			Returns memory type that meets requirements.
	 * </p>
	 * @param memoryProperties	- Memory properties.
	 * @param bits				- Interesting indices.
	 * @param properties		- Properties that memory type should meet.
	 * @param typeIndex			- Integer buffer for returned value.
	 * @return					- Information about successfulness of the operation(true - success, false - fail).
	 */
	 public static boolean getMemoryType(VkPhysicalDeviceMemoryProperties memoryProperties, int bits, int properties, IntBuffer typeIndex) {
		 for(int i = 0; i < 32; i++) 
			 if((bits & (1<<i)) > 0)
				 if((memoryProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
					 typeIndex.put(0, i);
					 return true;
				 }
		 
		 return false;
	 }
	 
	 /**
	     * <h5>Description:</h5>
		 * <p>
		 * 		Obtains <b><i><code>VkSurfaceCapabilitiesKHR</code></i></b>.
		 * </p>
	     * @param physicalDevice 	- Valid physical device.
	     * @param surface			- Valid surface.
	     * @return					Surface capabilities.
	     * @see {@link org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR}
	     */
	    public static VkSurfaceCapabilitiesKHR getSurfaceCapabilities(VkPhysicalDevice physicalDevice, long surface) {
	    	VkSurfaceCapabilitiesKHR pSurfaceCapabilities = VkSurfaceCapabilitiesKHR.calloc();
	    	int err = vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface, pSurfaceCapabilities);
	    	if(err != VK_SUCCESS)
	    		throw new AssertionError("Failed to obtain surface capabilities: " + Util.translateVulkanError(err));
	    	return pSurfaceCapabilities;
	    }
	 
	 /**
	  *	<h5>Description:</h5>
	  * <p>
	  * 	Obtains available surface extensions.
	  * </p>
	  * @param physicalDevice	- Physical device to obtain properties from.
	  * @param surface			- Surface handle.
	  * @return					- Buffer with <b><i><code>VkSurfaceFormatKHR</code></i></b>.
	  * @see 					{@link VkSurfaceFormatKHR}
	  */
	public static VkSurfaceFormatKHR.Buffer listSurfaceFormats(VkPhysicalDevice physicalDevice, long surface){
		 IntBuffer pCount = memAllocInt(1);
		 int err = vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, pCount, null);
		 if(err != VK_SUCCESS)
			 throw new AssertionError("Failed to enumerate surface formats: " + Util.translateVulkanError(err));
		 int count = pCount.get(0);
		 
		 VkSurfaceFormatKHR.Buffer out = VkSurfaceFormatKHR.calloc(count);
		 err = vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, pCount, out);
		 memFree(pCount);
		 if(err != VK_SUCCESS)
			 throw new AssertionError("Failed to obtain surface formats: " + Util.translateVulkanError(err));
		 
		 return out;
	 }
	
	/**
	 * <h5>Description:</h5>
	 * <p>
	 * 	Obtains desired color format if possible.
	 * </p>
	 * 
	 * @param physicalDevice		- Physical device to obtain informations from.
	 * @param surface				- Surface handle.
	 * @param first					- Index to start iterating from. Most likely <b>0</b>.
	 * @param desiredFormat			- Most suitable color format.
	 * @param desiredColorSpace		- Most suitable color space.
	 * @return						- Returns <b><i><code>ColorFormatAndSpace</code></i></b> that support <b>desiredColorFormat</b>
	 * 								if possible. Otherwise <b>null</b>.
	 */
	public static ColorFormatAndSpace getNextSurfaceFormat(PhysicalDevice physicalDevice, long surface, int first, int desiredFormat, int desiredColorSpace) {
		VkSurfaceFormatKHR.Buffer formats = listSurfaceFormats(physicalDevice, surface);
		ColorFormatAndSpace out = null;
		int size = formats.remaining();
		
		if(size == 1 && formats.get(0).format() == VK_FORMAT_UNDEFINED)
			return new ColorFormatAndSpace(desiredFormat, desiredColorSpace == -1 ? formats.get(0).colorSpace() : desiredColorSpace);
		
		for(int i = first; i < size; i++)
			if(formats.get(i).format() == desiredFormat || desiredFormat == -1) {
				out = new ColorFormatAndSpace(formats.get(i).format(), formats.get(i).colorSpace());
				
				if(formats.get(i).colorSpace() == desiredColorSpace || desiredColorSpace == -1)
					break;
			}
		
		formats.free();
		
		return out;
	}
	
	@Deprecated
	/**
	 * <h5>Description:</h5>
	  * <p>
	  * 	Obtains desired color space.
	  * </p>
	 * @param physicalDevice		- Physical device to obtain informations from.
	 * @param surface				- Surface handle.
	 * @param desiredColorFormat	- Most suitable color format.
	 * @return						- Returns <b><i><code>ColorFormatAndSpace</code></i></b> that support <b>desiredColorFormat</b>
	 * 								if possible. Or is the first available color format.
	 */
	public static ColorFormatAndSpace getColorFormatAndSpace(PhysicalDevice physicalDevice, long surface, int desiredColorFormat) { 

		//Check for compatibility.
		if(physicalDevice.getNextQueueFamilyIndex(0, VK_QUEUE_GRAPHICS_BIT) == -1)
			throw new AssertionError("Could not find queue family that would support graphics operations.");
		if(physicalDevice.getNextQueueFamilyIndexKHR(0, VK_QUEUE_GRAPHICS_BIT, surface) == -1)
			throw new AssertionError("No presentation queue found");

		ColorFormatAndSpace out = getNextSurfaceFormat(physicalDevice, surface, 0, desiredColorFormat, -1);
			 	
		if(out == null)
			out = getNextSurfaceFormat(physicalDevice, surface, 0, -1, -1);
		
		return out;
	}
	
	/**
	 * <h5>Description:</h5>
	 * <p>
	 * 		Converts file content to byte buffer.
	 * </p>
	 * @param file
	 * @return
	 */
	public static ByteBuffer fileToByteBuffer(File file) {
		 if(file.isDirectory())
			 return null;
		 
		 ByteBuffer buffer = null;
		 
		 try {
			 FileInputStream fis = new FileInputStream(file);
			 FileChannel fc = fis.getChannel();
			 buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
			 fc.close();
			 fis.close();

		 } catch (IOException e) {
			 e.printStackTrace();
		 }
		 
		 return buffer;
	 }
	
	/**
	 * <h5>Description:</h5>
	 * <p>
	 * 		Creates a shader module.
	 * </p>
	 * @param logicalDevice
	 * @param file
	 * @return
	 */
	public static long createShaderModule(VkDevice logicalDevice, File file) {
		ByteBuffer shaderData = fileToByteBuffer(file);
	 
		VkShaderModuleCreateInfo moduleCreateInfo  = VkShaderModuleCreateInfo.calloc()
				 .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
				 .flags(0)
				 .pNext(NULL)
				 .pCode(shaderData);
		 
		LongBuffer pShaderModule = memAllocLong(1);
		int err = vkCreateShaderModule(logicalDevice, moduleCreateInfo, null, pShaderModule);
		if(err != VK_SUCCESS)
			throw new AssertionError("Failed to create shader module: " + Util.translateVulkanError(err));
		long handle = pShaderModule.get(0);
		 
		memFree(pShaderModule);
		moduleCreateInfo.free();
		
		return handle;
	 }
	
	/**
	 * <h5>Description:</h5>
	 * <p>Creates <code><b><i>LogicalDevice</i></b></code> with given parameters.</p>
	 * @param dev					- <b>Must</b> be a valid <code><b><i>PhysicalDevice</i></b></code>.
	 * @param requiredQueueFlags	- Bitwise <b>OR</b> of required queue flags.
	 * @param layers				- Layers to be enabled.
	 * @param extensions			- Needed extensions.
	 * @return	<p>Valid LogicalDevice.</p>
	 * @see {@link core.employees.LogicalDevice}
	 */
	public static LogicalDevice createLogicalDevice(PhysicalDevice dev, int requiredQueueFlags, String[] layers, String[] extensions) {
		ByteBuffer[] bLayers = makeByteBuffers(layers);
		ByteBuffer[] bExtensions = makeByteBuffers(extensions);
		PointerBuffer pExtensions = makePointer(bExtensions);
		
		LogicalDevice logicalDevice = createLogicalDevice(dev, requiredQueueFlags, bLayers, pExtensions);
		
		memFree(pExtensions);
		
		freeBuffers(bLayers);
		freeBuffers(bExtensions);
		
		return logicalDevice;
	}
	
	/**
	 * <h5>Description:</h5>
	 * <p>Creates <code><b><i>LogicalDevice</i></b></code> with given parameters.</p>
	 * @param dev					- <b>Must</b> be a valid <code><b><i>PhysicalDevice</i></b></code>.
	 * @param requiredQueueFlags	- Bitwise <b>OR</b> of required queue flags.
	 * @param layers				- Layers to be enabled.
	 * @param extensions			- Needed extensions.
	 * @return	<p>Valid LogicalDevice.</p>
	 * @see {@link core.employees.LogicalDevice}
	 */
	public static LogicalDevice createLogicalDevice(PhysicalDevice dev, int requiredQueueFlags, ByteBuffer[] layers, PointerBuffer extensions) {
		LogicalDevice out = null;
		
		int desiredQueueIndex = dev.getNextQueueFamilyIndex(0, requiredQueueFlags);
		
		FloatBuffer queuePriorities = memAllocFloat(1).put(0.0f);
		queuePriorities.flip();
		
		PointerBuffer ppEnabledLayerNames = makePointer(layers);

		VkDeviceQueueCreateInfo.Buffer deviceQueueCreateInfo = VkDeviceQueueCreateInfo.calloc(1)
				.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
				.pNext(NULL)
				.queueFamilyIndex(desiredQueueIndex)
				.pQueuePriorities(queuePriorities);
		
		VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.calloc()
				.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
				.pNext(NULL)
				.flags(0)
				.pQueueCreateInfos(deviceQueueCreateInfo)
				.ppEnabledLayerNames(ppEnabledLayerNames)
				.ppEnabledExtensionNames(extensions);
		
		PointerBuffer pdev = memAllocPointer(1);
		int err = vkCreateDevice(dev, deviceCreateInfo, null, pdev);
		if(err < 0)
			throw new AssertionError("Failed to create logical device: " + translateVulkanError(err));
		
		long ldev = pdev.get(0);
		out = new LogicalDevice(ldev, dev, deviceCreateInfo);
		out.setGraphicsQueueFamilyIndex(desiredQueueIndex);

		deviceCreateInfo.free();
		deviceQueueCreateInfo.free();
		
		memFree(queuePriorities);
		memFree(ppEnabledLayerNames);
		
		memFree(pdev);
		
		return out;
	}
	
	/**
	 * <h5>Description:</h5>
	 * <p>Creates VkInsatnce.</p>
	 * @param appInfo				- Must be valid VkApplicationInfo.
	 * @param validationLayers		- Validation layers that should be enabled.
	 * @param requiredExtensions	- Required by GLFW Vulkan extensions.
	 * @param userExtensions		- Optional extensions.
	 * @return		<p>VkInstance</p>
	 * @see {@link org.lwjgl.vulkan.VkInstance}
	 * @see {@link org.lwjgl.vulkan.VkApplicationInfo}
	 */
	public static VkInstance createInstance(VkApplicationInfo appInfo, ByteBuffer[] validationLayers, PointerBuffer requiredExtensions, ByteBuffer... userExtensions) {		
		PointerBuffer ppEnabledLayerNames = memAllocPointer(validationLayers.length);
		for(int i = 0; HardwareManager.validation && i < validationLayers.length; i++)
			ppEnabledLayerNames.put(validationLayers[i]);
		ppEnabledLayerNames.flip();
		
		PointerBuffer extensions = memAllocPointer(requiredExtensions.remaining() + userExtensions.length);
		extensions.put(requiredExtensions);
		for(int i = 0; i < userExtensions.length; i++)
			extensions.put(userExtensions[i]);
		extensions.flip();
		
		VkInstanceCreateInfo instanceCreateInfo = VkInstanceCreateInfo.calloc()
				.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
				.pNext(NULL)
				.pApplicationInfo(appInfo)
				.ppEnabledExtensionNames(extensions)
				.ppEnabledLayerNames(ppEnabledLayerNames);
		
		PointerBuffer inst = memAllocPointer(1);
		int err = vkCreateInstance(instanceCreateInfo, null, inst);
		if(err != 0)
			throw new AssertionError("Could not create VkInstance. " + translateVulkanError(err));
		long instanceAdr = inst.get(0);
		VkInstance out = new VkInstance(instanceAdr, instanceCreateInfo);
		
		memFree(inst);
		memFree(extensions);
		memFree(ppEnabledLayerNames);
		
		return out;
	}
	
	static final String[] errorMessages = {
					//Success codes
						"Command successfully completed", 
						"A fence or query has not yet completed", 
						"A wait operation has not completed in the specified time", 
						"An event is signaled",
						"An event is unsignaled",
						"A return array was too small for the result",
						
					//Error codes
						"A host memory allocation has failed.",
						"A device memory allocation has failed.",
						"Initialization of an object could not be completed for implementation-specific reasons.",
						"The logical or physical device has been lost.",
						"Mapping of a memory object has failed.",
						"A requested layer is not present or could not be loaded.",
						"A requested extension is not supported.",
						"A requested feature is not supported.",
						"The requested version of Vulkan is not supported by the driver or is otherwise incompatible for implementation-specific reasons.",
						"Too many objects of the type have already been created.",
						"A requested format is not supported on this device.",
						"A pool allocation has failed due to fragmentation of the pool’s memory. This must only be returned if no attempt to allocate host or device memory was made to accomodate the new allocation."
						};
	/**
	 * <h5>Description:</h5>
	 * <p>Translates Vulkan error code to text.</p>
	 * @param err	- Vulkan error code.
	 * @return Translated error description.
	 */
	public static String translateVulkanError(int err) {
		//Success codes:
		if(err >= 0)
			return errorMessages[err];
		//Error codes:
		return errorMessages[5 - err];
	}
}
