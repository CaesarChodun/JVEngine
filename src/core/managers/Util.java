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
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkAllocationCallbacks;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkInstance;
import org.eclipse.jdt.annotation.Nullable;
import org.lwjgl.PointerBuffer;

/**
 * @author cezarychodun
 *
 */
public class Util {
	
	private static LongBuffer longBuffer = memAllocLong(1);
	
	
	public static class GPUBuffer{
		public long buffer;
		public long memory;
		public long allocationSize;
		
		public GPUBuffer() {}
		
		public GPUBuffer(long buffer, long memory, long allocationSize) {
			this.buffer = buffer;
			this.memory = memory;
			this.allocationSize = allocationSize;
		}
	};
	
	/**
	 * 
	 * @param texts
	 * @return
	 */
	public static ByteBuffer[] makeByteBuffers(String... texts) {
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
	public static PointerBuffer makePointer(ByteBuffer... buffers) {
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
	public static void freeBuffers(Buffer... buffers) {
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
		 * 			Returns memory type that meets requirements.
		 * </p>
		 * @param memoryProperties	- Memory properties.
		 * @param bits				- Interesting indices.
		 * @param properties		- Properties that memory type should meet.
		 * @return					- Returns memory type index that meets requirements or <b>-1</b> if none were found.
		 */
		 public static int getMemoryType(VkPhysicalDeviceMemoryProperties memoryProperties, int bits, int properties) {
			 for(int i = 0; i < 32; i++) 
				 if((bits & (1<<i)) > 0)
					 if((memoryProperties.memoryTypes(i).propertyFlags() & properties) == properties)
						 return i;
			 
			 return -1;
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
	
	private static PointerBuffer pCommandBuffers = memAllocPointer(1);
	
	/**
	 * <h5>Description:</h5>
	 * <p>
	 * 		Submits command buffers.
	 * </p>
	 * @param queue
	 * @param fence
	 * @param commandBuffers
	 */
	public static void submitCommandBuffers(VkQueue queue, long fence, VkCommandBuffer... commandBuffers) {
		
		for(int i = 0; i < commandBuffers.length; i++) {
			if(commandBuffers == null || commandBuffers[i].address() == NULL)
				throw new AssertionError("Command buffer is corrupted.");
			pCommandBuffers.put(commandBuffers[i]);
		}
		pCommandBuffers.flip(); //TODO: Fliping the buffer strongly affects performance(~9% in Primitive demo example).
		
		VkSubmitInfo pSubmitInfo = VkSubmitInfo.calloc()
				.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
				.pCommandBuffers(pCommandBuffers);
		
		int err = vkQueueSubmit(queue, pSubmitInfo, fence);
		if(err != VK_SUCCESS)
			throw new AssertionError("Failed to submit command buffers(" + commandBuffers.length + "): " + Util.translateVulkanError(err));
		
		memFree(pCommandBuffers);
		pSubmitInfo.free();
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
	public static ColorFormatAndSpace getNextColorFormatAndSpace(int first, PhysicalDevice physicalDevice, long surface, int desiredFormat, int desiredColorSpace) {
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

		ColorFormatAndSpace out = getNextColorFormatAndSpace(0, physicalDevice, surface, desiredColorFormat, -1);
			 	
		if(out == null)
			out = getNextColorFormatAndSpace(0, physicalDevice, surface, -1, -1);
		
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
	 * 		Obtains queue from logical device.
	 * </p>
	 * @param device
	 * @param queueFamilyIndex
	 * @param queueIndex		- If only queue family capabilities are considered queue index should equal <b>0</b>.
	 * @return
	 */
	public static VkQueue getDeviceQueue(VkDevice device, int queueFamilyIndex, int queueIndex) {
		PointerBuffer pQueue = memAllocPointer(1);
		vkGetDeviceQueue(device, queueFamilyIndex, queueIndex, pQueue);
		long queueHandle = pQueue.get(0);
		memFree(pQueue);
		return new VkQueue(queueHandle, device);
	}
	
	/**
	 * <h5>Description:</h5>
	 * <p>
	 * 		Obtains best compatible memory type.
	 * </p>
	 * @param memoryProperties		- Memory properties from physical device.
	 * @param typeBits				- Memory requirements.
	 * @param flags					- Flags that memory type should contain.
	 * @return						- Index of suitable memory type.
	 */
	public static int getMemoryTypeFromProperties(VkPhysicalDeviceMemoryProperties memoryProperties, int typeBits, int flags) {		
		for(int i = 0; i < memoryProperties.memoryTypeCount(); i++) {
			if((typeBits&1) > 0) {
				if((memoryProperties.memoryTypes(i).propertyFlags() & flags) == flags)
					return i;
			}
			typeBits >>= 1;
		}
		return -1;
	}
	/**
	 * <h5>Description:</h5>
	 * <p>
	 * 		Obtains best compatible memory type.
	 * </p>
	 * @param physicalDevice		- Valid physical device.
	 * @param memoryRequirements	- Memory requirements.
	 * @param flags					- Flags that memory type should contain.
	 * @return						- Index of suitable memory type.
	 */
	public static int getMemoryTypeFromProperties(PhysicalDevice physicalDevice, VkMemoryRequirements memoryRequirements, int flags) {
		return getMemoryTypeFromProperties(physicalDevice.memoryProperties, memoryRequirements.memoryTypeBits(), flags);
	}
	
	/**
	 * <h5>Description:</h5>
	 * <p>
	 * 		Allocates and then binds image memory.
	 * </p>
	 * @param physicalDevice	- Valid physical device.
	 * @param device			- Valid <b>VkDevice</b>.
	 * @param pAllocator		- Valid allocator or <b>null</b>.
	 * @param image				- Image handle.
	 * @param memoryTypeFlags	- Flags for memory type.
	 * @param offset			- Offset for binding memory.
	 * @return					- pointer to <b>VkMemory</b> object.
	 */
	public static long allocAndBindImageMemory(PhysicalDevice physicalDevice, VkDevice device, VkAllocationCallbacks pAllocator, long image, int memoryTypeFlags, int offset) {
		VkMemoryRequirements pMemoryRequirements = VkMemoryRequirements.calloc();
		vkGetImageMemoryRequirements(device, image, pMemoryRequirements);
		VkMemoryAllocateInfo memAllocInfo = VkMemoryAllocateInfo.calloc()
				.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
				.pNext(NULL)
				.allocationSize(pMemoryRequirements.size())
				.memoryTypeIndex(Util.getMemoryTypeFromProperties(physicalDevice, pMemoryRequirements, memoryTypeFlags));
		
		LongBuffer pMemory = memAllocLong(1);
		int err = vkAllocateMemory(device, memAllocInfo, pAllocator, pMemory);
		if(err != VK_SUCCESS)
			throw new AssertionError("Failed to allocate memory: " + Util.translateVulkanError(err));
		long memory = pMemory.get(0);
		memFree(pMemory);		
		
		err = vkBindImageMemory(device, image, memory, offset);
		if(err != VK_SUCCESS)
			throw new AssertionError("Failed to bind memory:" + Util.translateVulkanError(err));
		
		return memory;
	}
	
	public static long createBufferData(LogicalDevice device, VkPhysicalDeviceMemoryProperties physicalDeviceMemoryProperties, ByteBuffer data, int dataSize, int bufferUsage, int sharingMode) {
		
		
		VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.calloc()
				.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
				.pNext(NULL)
				.flags(0)
				.usage(bufferUsage)
				.size(dataSize)
				.pQueueFamilyIndices(null)
				.sharingMode(sharingMode);
		
		LongBuffer pBuffer = memAllocLong(1);
		int err = vkCreateBuffer(device, bufferCreateInfo, null, pBuffer);
		if(err != VK_SUCCESS)
			throw new AssertionError("Failed to create buffer: " + Util.translateVulkanError(err));
		long buffer = pBuffer.get(0);
		memFree(pBuffer);
		
		
		VkMemoryRequirements memoryRequirements = VkMemoryRequirements.calloc();
		vkGetBufferMemoryRequirements(device, buffer, memoryRequirements);

		VkMemoryAllocateInfo memAlloc = VkMemoryAllocateInfo.calloc()
				.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
				.pNext(NULL)
				.allocationSize(memoryRequirements.size())
				.memoryTypeIndex(Util.getMemoryType(physicalDeviceMemoryProperties, memoryRequirements.memoryTypeBits(), VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT));
		
		LongBuffer pMemory = memAllocLong(1);
		err = vkAllocateMemory(device, memAlloc, null, pMemory);
		if(err != VK_SUCCESS)
			throw new AssertionError("Failed to allocate memory: " + Util.translateVulkanError(err));
		long memory = pMemory.get(0);
		memFree(pMemory);
		
		PointerBuffer ppData = memAllocPointer(1);
		err = vkMapMemory(device, memory, 0, memAlloc.allocationSize(), 0, ppData);
		if(err != VK_SUCCESS)
			throw new AssertionError("Failed to map memory: " + Util.translateVulkanError(err));
		long pData = ppData.get(0);
		
		memCopy(memAddress(data), pData, data.remaining());
		
		vkUnmapMemory(device, memory);
		
		err = vkBindBufferMemory(device, buffer, memory, 0);
		if(err != VK_SUCCESS)
			throw new AssertionError("Failed to bind buffer memory: " + Util.translateVulkanError(err));
		
		memFree(ppData);
		bufferCreateInfo.free();
		
		return buffer;
	}
	
	/**
	 * <h5>Description:</h5>
	 * <p>
	 * 		Allocates buffer memory for future use.
	 * </p>
	 * @param device							- Logical device
	 * @param physicalDeviceMemoryProperties	- Physical device
	 * @param buferSize							- Buffer size
	 * @param bufferUsage						- Buffer usage 
	 * @param sharingMode						- Sharing mode
	 * @return									- returns GPUBuffer allocated from given logical device.
	 */
	public static GPUBuffer allocateBufferMemoryGPU(LogicalDevice device, VkPhysicalDeviceMemoryProperties physicalDeviceMemoryProperties, int buferSize, int bufferUsage, int sharingMode) {
		GPUBuffer ans = new GPUBuffer();
		
		VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.calloc()
				.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
				.pNext(NULL)
				.flags(0)
				.usage(bufferUsage)
				.size(buferSize)
				.pQueueFamilyIndices(null)
				.sharingMode(sharingMode);
		
		LongBuffer pBuffer = memAllocLong(1);
		int err = vkCreateBuffer(device, bufferCreateInfo, null, pBuffer);
		if(err != VK_SUCCESS)
			throw new AssertionError("Failed to create buffer: " + Util.translateVulkanError(err));
		ans.buffer = pBuffer.get(0);
		memFree(pBuffer);
		
		VkMemoryRequirements memoryRequirements = VkMemoryRequirements.calloc();
		vkGetBufferMemoryRequirements(device, ans.buffer, memoryRequirements);
		ans.allocationSize = memoryRequirements.size();

		VkMemoryAllocateInfo memAlloc = VkMemoryAllocateInfo.calloc()
				.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
				.pNext(NULL)
				.allocationSize(ans.allocationSize)
				.memoryTypeIndex(Util.getMemoryType(physicalDeviceMemoryProperties, memoryRequirements.memoryTypeBits(), VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT));
		
		LongBuffer pMemory = memAllocLong(1);
		err = vkAllocateMemory(device, memAlloc, null, pMemory);
		if(err != VK_SUCCESS)
			throw new AssertionError("Failed to allocate memory: " + Util.translateVulkanError(err));
		ans.memory = pMemory.get(0);
		memFree(pMemory);
		

		memoryRequirements.free();
		memAlloc.free();
		
		return ans;
	}
	
	/**
	 * <h5>Description:</h5>
	 * <p>
	 * 		Binds data to gpu buffer.
	 * </p>
	 * @param device	- Logical device
	 * @param buff		- Destination gpu buffer
	 * @param data		- Source buffer
	 */
	public static void bindBufferMemoryGPU(LogicalDevice device, GPUBuffer buff, ByteBuffer data) {
		if(device == null)
			System.err.println("Fuck!!!!");
		PointerBuffer ppData = memAllocPointer(1);
		int err = vkMapMemory(device, buff.memory, 0, buff.allocationSize, 0, ppData);
		if(err != VK_SUCCESS)
			throw new AssertionError("Failed to map memory: " + Util.translateVulkanError(err));
		long pData = ppData.get(0);
		memFree(ppData);
		
		memCopy(memAddress(data), pData, data.remaining());
		
		vkUnmapMemory(device, buff.memory);
		
		err = vkBindBufferMemory(device, buff.buffer, buff.memory, 0);
		if(err != VK_SUCCESS)
			throw new AssertionError("Failed to bind buffer memory: " + Util.translateVulkanError(err));
	}
	
	/**
	 * <h5>Description:</h5>
	 * <p>
	 * 		Creates semaphore.
	 * </p>
	 * @param device 	- Logical device
	 * @param ci		- Semaphore create info
	 * @param alloc		- Allocation callbacks
	 * @return			- Handle to the semaphore
	 */
	public static long createSemaphore(VkDevice device, VkSemaphoreCreateInfo ci, VkAllocationCallbacks alloc) {		
		int err = vkCreateSemaphore(device, ci, alloc, longBuffer);
		if(err != VK_SUCCESS)
			throw new AssertionError("Failed to acquire semaphore: " + translateVulkanError(err));
		
		return longBuffer.get(0);
	}
	
	/**
	 * <h5>Description:</h5>
	 * <p>
	 * 		Creates fence.
	 * </p>
	 * @param device	- Logical device
	 * @param ci		- Fence create info
	 * @param alloc		- Allocation callbacks
	 * @return			- Handle to the fence
	 */
	public static long createFence(VkDevice device, VkFenceCreateInfo ci, VkAllocationCallbacks alloc) {
		int err = vkCreateFence(device, ci, null, longBuffer);
		if(err != VK_SUCCESS)
			throw new AssertionError("Failed to acquire semaphore: " + translateVulkanError(err));
		
		return longBuffer.get(0);
	}
	
	/**
	 * <h5>Description:</h5>
	 * <p>
	 * 		Allocates gpu visible buffer based on given <i><b>data</b></i> buffer.
	 * </p>
	 * @param device							- Logical device
	 * @param physicalDeviceMemoryProperties	- Memory properties
	 * @param data								- <code>ByteBuffer</code> that contains data
	 * @param dataSize							- Size of the data
	 * @param bufferUsage						- Buffer usage
	 * @param sharingMode						- Sharing mode
	 * @return									- Handle to the gpu visible buffer
	 */
	public static long createBufferData(LogicalDevice device, VkPhysicalDeviceMemoryProperties physicalDeviceMemoryProperties, ByteBuffer data, int dataSize, int bufferUsage, int sharingMode) {
		
		GPUBuffer ans = allocateBufferMemoryGPU(device, physicalDeviceMemoryProperties, dataSize, bufferUsage, sharingMode);
		bindBufferMemoryGPU(device, ans, data);
		
		return ans.buffer;
	}

	/**
	 * <h5>Description:</h5>
	 * <p>
	 * 		Creates descriptor set layout.
	 * </p>
	 * @param device		- Logical device
	 * @param layoutBinding	- Layout binding
	 * @return				- Handle to descriptor set layout
	 */
	public static long createDescriptorSetLayout(VkDevice device, VkDescriptorSetLayoutBinding.Buffer layoutBinding) {
		VkDescriptorSetLayoutCreateInfo layoutCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc()
				.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
				.pNext(NULL)
				.pBindings(layoutBinding);
		
		LongBuffer pSetLayout = memAllocLong(1);
		int err = vkCreateDescriptorSetLayout(device, layoutCreateInfo, null, pSetLayout);
		if(err != VK_SUCCESS)
			throw new AssertionError("Filed to create descriptor set layout: " + Util.translateVulkanError(err));
		long ans = pSetLayout.get(0);
		memFree(pSetLayout);
		layoutCreateInfo.free();
		return ans;
	}
	
	/**
	 * <h5>Description:</h5>
	 * <p>
	 * 		Creates c0mmand buffers.
	 * </p>
	 * @param device
	 * @param commandPool
	 * @param level			- See <b>VkCommandPoolCreateFlags</b>
	 * @param count
	 * @return				- Array of command buffers. With size equal to <b><i>count</i></b>.
	 */
	public static VkCommandBuffer[] createCommandBuffers(VkDevice device, long commandPool, int level, int count) {
		VkCommandBufferAllocateInfo cmdAlloc = VkCommandBufferAllocateInfo.calloc()
				.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
				.pNext(NULL)
				.level(level)
				.commandPool(commandPool)
				.commandBufferCount(count);
		
		PointerBuffer pCommandBuffers = memAllocPointer(count);
		
		int err = vkAllocateCommandBuffers(device, cmdAlloc, pCommandBuffers);
		if(err != VK_SUCCESS)
			throw new AssertionError("Failed to allocate command buffers: " + Util.translateVulkanError(err));
		
		VkCommandBuffer[] buffers = new VkCommandBuffer[count];
		for(int i = 0; i < count; i++)
			buffers[i] = new VkCommandBuffer(pCommandBuffers.get(i), device);
		
		memFree(pCommandBuffers);
		cmdAlloc.free();
		
		return buffers;
	}
	
	/**
	 * <h5>Description:</h5>
	 * <p>
	 * 		Creates new command pool.
	 * </p>
	 * @param device
	 * @param queueFamilyIndex
	 * @param flags
	 * @return					- Pipeline handle.
	 */
	public static long createCommandPool(VkDevice device, int queueFamilyIndex, int flags) {
		VkCommandPoolCreateInfo createInfo = VkCommandPoolCreateInfo.calloc()
				.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
				.pNext(NULL)
				.flags(flags)
				.queueFamilyIndex(queueFamilyIndex);
		
		LongBuffer pCommandPool = memAllocLong(1);
		int err = vkCreateCommandPool(device, createInfo, null, pCommandPool);
		if(err != VK_SUCCESS)
			throw new AssertionError("Failed to create command pool: " + Util.translateVulkanError(err));
		
		long out = pCommandPool.get(0);
		
		memFree(pCommandPool);
		createInfo.clear();
		
		return out;
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
	 * <p>
	 * 		Creates shader stage.
	 * </p>
	 * @param shaderModule	- Shader module.
	 * @param stage			- Shader stage.
	 * @param invokeName	- Name of the method to be invoked.
	 * @return
	 */
	public static VkPipelineShaderStageCreateInfo createShaderStage(long shaderModule, int stage, String invokeName) {
		 VkPipelineShaderStageCreateInfo shaderStage = VkPipelineShaderStageCreateInfo.calloc()
				 .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
				 .pNext(NULL)
				 .stage(stage)
				 .module(shaderModule)
				 .pName(memUTF8(invokeName));
		 return shaderStage;
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
	@Deprecated
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
	@Deprecated
	public static LogicalDevice createLogicalDevice(PhysicalDevice dev, int requiredQueueFlags, ByteBuffer[] layers, PointerBuffer extensions) {
		LogicalDevice out = null;
		
		int desiredQueueIndex = dev.getNextQueueFamilyIndex(0, requiredQueueFlags);
		System.out.println("Queue: " + desiredQueueIndex);
		
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
		out.setQueueFamilyIndex(desiredQueueIndex);

		deviceCreateInfo.free();
		deviceQueueCreateInfo.free();
		
		memFree(queuePriorities);
		memFree(ppEnabledLayerNames);
		
		memFree(pdev);
		
		return out;
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
	public static LogicalDevice createLogicalDevice(PhysicalDevice dev, int queueFamilyIndex, FloatBuffer queuePriorities, int flags, @Nullable ByteBuffer[] layers, @Nullable PointerBuffer extensions) {
		LogicalDevice out = null;
		
		PointerBuffer ppEnabledLayerNames = (layers == null) ? null : makePointer(layers);
		
		VkDeviceQueueCreateInfo.Buffer deviceQueueCreateInfo = VkDeviceQueueCreateInfo.calloc(1)
				.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
				.pNext(NULL)
				.queueFamilyIndex(queueFamilyIndex)
				.pQueuePriorities(queuePriorities);
		
		VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.calloc()
				.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
				.pNext(NULL)
				.flags(flags)
				.pQueueCreateInfos(deviceQueueCreateInfo)
				.ppEnabledLayerNames(ppEnabledLayerNames)
				.ppEnabledExtensionNames(extensions);
		
		PointerBuffer pdev = memAllocPointer(1);
		int err = vkCreateDevice(dev, deviceCreateInfo, null, pdev);
		if(err < 0)
			throw new AssertionError("Failed to create logical device: " + translateVulkanError(err));
		
		long ldev = pdev.get(0);
		out = new LogicalDevice(ldev, dev, deviceCreateInfo);
		out.setQueueFamilyIndex(queueFamilyIndex);

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
		if(err <= -1000000)
			return "Error code: " + err;
		return errorMessages[5 - err];
	}
}
